package com.researchoffice.orders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.researchoffice.TestcontainersConfiguration;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
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
class ExpertFlowTests {

    @Autowired
    MockMvc mvc;

    private String body(RequestBuilder req) throws Exception {
        return mvc.perform(req).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private String login(String email, String password) throws Exception {
        String json = body(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)));
        return "Bearer " + JsonPath.read(json, "$.accessToken");
    }

    private String createExpert(String admin, String email) throws Exception {
        String json = body(post("/api/admin/staff").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"fullName\":\"د. أحمد\",\"password\":\"Expert#2026x\",\"role\":\"EXPERT\"}"
                        .formatted(email)));
        return JsonPath.read(json, "$.id");
    }

    private String json(String admin, String url, String content) throws Exception {
        return body(post(url).header("Authorization", admin).contentType(MediaType.APPLICATION_JSON).content(content));
    }

    @Test
    void expertWorksOnlyOnAssignedTasksAndAdminPublishesTheResult() throws Exception {
        String admin = login("admin@research-office.local", "ChangeMe#2026");
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String expertId = createExpert(admin, "expert-" + suffix + "@ro.test");
        createExpert(admin, "other-" + suffix + "@ro.test");
        String expert = login("expert-" + suffix + "@ro.test", "Expert#2026x");
        String other = login("other-" + suffix + "@ro.test", "Expert#2026x");

        // a client request, quoted and accepted
        List<String> ids = JsonPath.read(body(get("/api/public/services")), "$[?(@.code == 'STAT_ANALYSIS')].id");
        String created = body(post("/api/public/orders").contentType(MediaType.APPLICATION_JSON).content("""
                {"serviceId":"%s","clientName":"عميل الخبير","clientPhone":"+97455550000","details":"تحليل بيانات 200 مشارك"}
                """.formatted(ids.getFirst())));
        String token = JsonPath.read(created, "$.trackingToken");
        List<String> orderIds = JsonPath.read(body(get("/api/admin/orders").param("q", "عميل الخبير")
                .header("Authorization", admin)), "$.items[*].id");
        String order = "/api/admin/orders/" + orderIds.getFirst();
        json(admin, order + "/quote", "{\"price\":300,\"currency\":\"QAR\",\"note\":null}");
        mvc.perform(post("/api/public/orders/" + token + "/accept-quote")).andExpect(status().isOk());

        // admin assigns the expert
        mvc.perform(post(order + "/assign").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expertId\":\"" + expertId + "\"}"))
                .andExpect(jsonPath("$.assignedExpert.fullName").value("د. أحمد"));

        // the expert sees the task, without the client's contact details
        String task = "/api/expert/tasks/" + orderIds.getFirst();
        mvc.perform(get("/api/expert/tasks").header("Authorization", expert)).andExpect(jsonPath("$.length()").value(1));
        String detail = body(get(task).header("Authorization", expert));
        assertThat(detail).doesNotContain("+97455550000").doesNotContain("عميل الخبير").doesNotContain("QAR");
        assertThat((Boolean) JsonPath.read(detail, "$.canStart")).isTrue();

        // another expert cannot see it, and experts cannot use the admin API
        mvc.perform(get(task).header("Authorization", other)).andExpect(status().isNotFound());
        mvc.perform(get("/api/admin/orders").header("Authorization", expert)).andExpect(status().isForbidden());

        // start → upload work → submit for review
        mvc.perform(post(task + "/start").header("Authorization", expert)).andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        String afterUpload = body(multipart(task + "/files")
                .file(new MockMultipartFile("files", "analysis.docx", "application/octet-stream", "x".getBytes()))
                .header("Authorization", expert));
        List<String> fileIds = JsonPath.read(afterUpload, "$.files[?(@.name == 'analysis.docx')].id");
        mvc.perform(post(task + "/submit").header("Authorization", expert).contentType(MediaType.APPLICATION_JSON)
                .content("{\"note\":\"جاهز للمراجعة\"}")).andExpect(jsonPath("$.status").value("IN_REVIEW"));

        // the client does not see the draft until the admin publishes it
        assertThat(body(get("/api/public/orders/" + token))).doesNotContain("analysis.docx");
        mvc.perform(post(order + "/files/" + fileIds.getFirst() + "/publish").header("Authorization", admin))
                .andExpect(status().isOk());
        String clientView = body(get("/api/public/orders/" + token));
        assertThat(clientView).contains("analysis.docx").contains("فريق المكتب").doesNotContain("د. أحمد");
    }
}
