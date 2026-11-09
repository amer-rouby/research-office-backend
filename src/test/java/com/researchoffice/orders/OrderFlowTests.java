package com.researchoffice.orders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.researchoffice.TestcontainersConfiguration;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

@SpringBootTest(properties = "ro.files.dir=target/test-files")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OrderFlowTests {

    @Autowired
    MockMvc mvc;

    private String admin;

    @BeforeEach
    void signIn() throws Exception {
        String json = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@research-office.local\",\"password\":\"ChangeMe#2026\"}"))
                .andReturn().getResponse().getContentAsString();
        admin = "Bearer " + JsonPath.read(json, "$.accessToken");
    }

    private String body(RequestBuilder req) throws Exception {
        return mvc.perform(req).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private String serviceId() throws Exception {
        String services = body(get("/api/public/services"));
        List<String> ids = JsonPath.read(services, "$[?(@.code == 'STAT_ANALYSIS')].id");
        return ids.getFirst();
    }

    private static MockMultipartFile file(String name) {
        return new MockMultipartFile("files", name, "application/pdf", "data".getBytes());
    }

    @Test
    void fullLifecycleFromRequestToConfirmedDelivery() throws Exception {
        // client submits a request
        String created = mvc.perform(post("/api/public/orders").contentType(MediaType.APPLICATION_JSON).content("""
                        {"serviceId":"%s","clientName":"محمد علي","clientPhone":"+974 5555 1234",
                         "clientEmail":null,"country":"قطر","details":"تحليل استبيان من 40 عبارة على 300 مشارك","deadline":null}
                        """.formatted(serviceId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reference").value(org.hamcrest.Matchers.startsWith("RO-")))
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(created, "$.trackingToken");
        String track = "/api/public/orders/" + token;

        mvc.perform(multipart(track + "/files").file(file("survey.pdf"))).andExpect(status().isOk());
        mvc.perform(get(track)).andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.files.length()").value(1));

        // staff finds it and quotes
        String page = body(get("/api/admin/orders").param("status", "NEW").header("Authorization", admin));
        List<String> ids = JsonPath.read(page, "$.items[?(@.clientName == 'محمد علي')].id");
        String id = ids.getFirst();
        String adminOrder = "/api/admin/orders/" + id;

        mvc.perform(post(adminOrder + "/quote").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"price\":450,\"currency\":\"qar\",\"note\":\"خلال 5 أيام\"}"))
                .andExpect(jsonPath("$.status").value("QUOTED"))
                .andExpect(jsonPath("$.quote.currency").value("QAR"))
                .andExpect(jsonPath("$.trackingUrl").value(org.hamcrest.Matchers.endsWith("/t/" + token)));

        // client accepts; staff works, reviews, delivers
        mvc.perform(post(track + "/accept-quote")).andExpect(jsonPath("$.status").value("ACCEPTED"));
        changeStatus(adminOrder, "IN_PROGRESS");
        mvc.perform(multipart(adminOrder + "/files").file(file("internal-notes.pdf")).param("kind", "INTERNAL")
                .header("Authorization", admin)).andExpect(status().isOk());
        changeStatus(adminOrder, "IN_REVIEW");
        String detail = body(multipart(adminOrder + "/files").file(file("results.pdf")).param("kind", "DELIVERABLE")
                .header("Authorization", admin));
        changeStatus(adminOrder, "DELIVERED");

        // client sees the deliverable but never the internal file, and can download it
        String view = body(get(track));
        assertThat((List<String>) JsonPath.read(view, "$.files[*].name")).containsExactly("survey.pdf", "results.pdf");
        assertThat((List<?>) JsonPath.read(view, "$.timeline[?(@.note == 'internal-notes.pdf')]")).isEmpty();
        List<String> deliverable = JsonPath.read(detail, "$.files[?(@.name == 'results.pdf')].id");
        List<String> internal = JsonPath.read(detail, "$.files[?(@.name == 'internal-notes.pdf')].id");
        mvc.perform(get(track + "/files/" + deliverable.getFirst())).andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("results.pdf")));
        mvc.perform(get(track + "/files/" + internal.getFirst())).andExpect(status().isNotFound());

        mvc.perform(post(track + "/confirm")).andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test
    void lifecycleRejectsSkippingSteps() throws Exception {
        String created = body(post("/api/public/orders").contentType(MediaType.APPLICATION_JSON).content("""
                {"serviceId":"%s","clientName":"سارة","clientPhone":"55551234","details":"تدقيق رسالة ماجستير 120 صفحة"}
                """.formatted(serviceId())));
        String token = JsonPath.read(created, "$.trackingToken");

        mvc.perform(post("/api/public/orders/" + token + "/confirm"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("INVALID_TRANSITION"));
    }

    @Test
    void rejectsExecutableUploadsAndUnknownLinks() throws Exception {
        String created = body(post("/api/public/orders").contentType(MediaType.APPLICATION_JSON).content("""
                {"serviceId":"%s","clientName":"خالد","clientPhone":"55551234","details":"دراسة جدوى لمطعم في الدوحة"}
                """.formatted(serviceId())));
        String token = JsonPath.read(created, "$.trackingToken");

        mvc.perform(multipart("/api/public/orders/" + token + "/files")
                        .file(new MockMultipartFile("files", "run.exe", "application/octet-stream", "x".getBytes())))
                .andExpect(jsonPath("$.code").value("FILE_TYPE_NOT_ALLOWED"));
        mvc.perform(get("/api/public/orders/not-a-real-token")).andExpect(status().isNotFound());
    }

    @Test
    void adminOrdersNeedAToken() throws Exception {
        mvc.perform(get("/api/admin/orders")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/orders/stats").header("Authorization", admin))
                .andExpect(jsonPath("$.byStatus.NEW").isNumber());
    }

    @Test
    void guardsStopStepsThatAreNotReady() throws Exception {
        String created = body(post("/api/public/orders").contentType(MediaType.APPLICATION_JSON).content("""
                {"serviceId":"%s","clientName":"حارس الخطوات","clientPhone":"55559999","details":"اختبار شروط الخطوات"}
                """.formatted(serviceId())));
        String token = JsonPath.read(created, "$.trackingToken");
        List<String> ids = JsonPath.read(body(get("/api/admin/orders").param("q", "حارس الخطوات")
                .header("Authorization", admin)), "$.items[*].id");
        String order = "/api/admin/orders/" + ids.getFirst();
        mvc.perform(post(order + "/quote").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"price\":500,\"currency\":\"SAR\",\"note\":null}")).andExpect(status().isOk());
        mvc.perform(post("/api/public/orders/" + token + "/accept-quote")).andExpect(status().isOk());

        // cannot pay more than the quote
        mvc.perform(post(order + "/payments").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":600,\"method\":\"CASH\",\"paidOn\":\"2026-11-09\",\"note\":null}"))
                .andExpect(jsonPath("$.code").value("PAYMENT_EXCEEDS_BALANCE"));
        mvc.perform(post(order + "/payments").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":200,\"method\":\"CASH\",\"paidOn\":\"2026-11-09\",\"note\":null}"))
                .andExpect(jsonPath("$.paidTotal").value(200));

        // cannot deliver without a file for the client
        changeStatus(order, "IN_PROGRESS");
        changeStatus(order, "IN_REVIEW");
        mvc.perform(post(order + "/status").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DELIVERED\",\"note\":null,\"visibleToClient\":true}"))
                .andExpect(jsonPath("$.code").value("NO_DELIVERABLES"));

        // dashboard money: SAR owed = 500 - 200 on accepted work
        String stats = body(get("/api/admin/orders/stats").header("Authorization", admin));
        List<Number> owed = JsonPath.read(stats, "$.money[?(@.currency == 'SAR')].outstanding");
        assertThat(owed.getFirst().doubleValue()).isEqualTo(300.0);
    }

    private void changeStatus(String adminOrder, String to) throws Exception {
        mvc.perform(post(adminOrder + "/status").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"%s\",\"note\":null,\"visibleToClient\":true}".formatted(to)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(to));
    }
}
