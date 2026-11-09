package com.researchoffice.notifications;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.researchoffice.TestcontainersConfiguration;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

@SpringBootTest(properties = {
    "ro.notifications.mail-host=smtp.test",
    "ro.notifications.from=office@ro.test",
    "ro.notifications.admin-email=owner@ro.test",
    "ro.files.dir=target/test-files"
})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class NotificationTests {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    JavaMailSender mailSender;

    private String body(RequestBuilder req) throws Exception {
        return mvc.perform(req).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    void officeHearsAboutNewRequestsAndClientAboutQuotesAndPayments() throws Exception {
        List<String> ids = JsonPath.read(body(get("/api/public/services")), "$[?(@.code == 'FEASIBILITY')].id");
        String created = body(post("/api/public/orders").contentType(MediaType.APPLICATION_JSON).content("""
                {"serviceId":"%s","clientName":"نورة","clientPhone":"+97455550001",
                 "clientEmail":"noura@client.test","details":"دراسة جدوى لمقهى في الدوحة"}
                """.formatted(ids.getFirst())));
        String reference = JsonPath.read(created, "$.reference");

        var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, timeout(5000)).send(captor.capture());
        SimpleMailMessage toOffice = captor.getValue();
        assertThat(toOffice.getTo()).containsExactly("owner@ro.test");
        assertThat(toOffice.getSubject()).contains("طلب جديد").contains(reference);
        assertThat(toOffice.getText()).contains("/admin/requests/");

        // quote + payment reach the client
        String admin = "Bearer " + JsonPath.read(body(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"admin@research-office.local\",\"password\":\"ChangeMe#2026\"}")), "$.accessToken");
        List<String> orderIds = JsonPath.read(body(get("/api/admin/orders").param("q", reference)
                .header("Authorization", admin)), "$.items[*].id");
        String order = "/api/admin/orders/" + orderIds.getFirst();

        mvc.perform(post(order + "/payments").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":100,\"method\":\"BANK_TRANSFER\",\"paidOn\":\"2026-11-09\",\"note\":null}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("NO_QUOTE"));
        mvc.perform(post(order + "/quote").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"price\":2000,\"currency\":\"QAR\",\"note\":null}")).andExpect(status().isOk());
        mvc.perform(post(order + "/payments").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":1000,\"method\":\"BANK_TRANSFER\",\"paidOn\":\"2026-11-09\",\"note\":\"مقدم\"}"))
                .andExpect(jsonPath("$.paidTotal").value(1000))
                .andExpect(jsonPath("$.payments.length()").value(1));

        verify(mailSender, timeout(5000).atLeast(3)).send(captor.capture());
        List<SimpleMailMessage> toClient = captor.getAllValues().stream()
                .filter(m -> List.of(m.getTo()).contains("noura@client.test")).toList();
        assertThat(toClient).extracting(SimpleMailMessage::getSubject)
                .anyMatch(s -> s.contains("عرض السعر"))
                .anyMatch(s -> s.contains("دفعتك"));
        String token = JsonPath.read(created, "$.trackingToken");
        mvc.perform(get("/api/public/orders/" + token)).andExpect(jsonPath("$.paidTotal").value(1000));
        verify(mailSender, atLeastOnce()).send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));
    }
}
