package com.researchoffice.notifications.internal;

import com.researchoffice.orders.OrderNotice;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;

/**
 * Turns request events into emails: the office is told about client actions,
 * the client (when they gave an email) is told about quotes, deliveries and payments.
 * Runs asynchronously after the request's transaction commits.
 */
@Component
@RequiredArgsConstructor
class OrderNoticeListener {

    private final MailGateway mail;

    @ApplicationModuleListener
    void on(OrderNotice n) {
        switch (n.type()) {
            case CREATED -> toOffice(n, "طلب جديد", "وصل طلب جديد من %s.".formatted(n.clientName()));
            case ACCEPTED -> toOffice(n, "تم قبول عرض السعر", "%s قبل عرض السعر. وزّع الطلب على خبير.".formatted(n.clientName()));
            case SUBMITTED_FOR_REVIEW -> toOffice(n, "عمل جاهز للمراجعة", "الخبير أرسل العمل للمراجعة.");
            case REVISION_REQUESTED -> toOffice(n, "طلب تعديل", "%s طلب تعديلًا.".formatted(n.clientName()));
            case CLOSED -> toOffice(n, "تم إغلاق الطلب", "تم إغلاق الطلب.");
            case QUOTED -> toClient(n, "عرض السعر لطلبك", "عرض السعر لطلبك جاهز. راجعه واضغط قبول للبدء.");
            case DELIVERED -> toClient(n, "ملفاتك جاهزة", "أنهينا العمل على طلبك وملفاتك جاهزة للتحميل.");
            case PAYMENT_RECORDED -> toClient(n, "تم استلام دفعتك", "شكرًا لك، تم تسجيل دفعتك.");
        }
    }

    private void toOffice(OrderNotice n, String title, String line) {
        mail.send(mail.adminEmail(), subject(title, n),
                """
                %s

                الطلب: %s
                الخدمة: %s
                %s
                افتح الطلب: %s
                """.formatted(line, n.reference(), n.serviceName(), detail(n), n.adminUrl()));
    }

    private void toClient(OrderNotice n, String title, String line) {
        mail.send(n.clientEmail(), subject(title, n),
                """
                مرحبًا %s،

                %s
                %s
                تابع طلبك من هنا: %s

                مع تحيات فريق مكتب الخدمات البحثية
                """.formatted(n.clientName(), line, detail(n), n.trackingUrl()));
    }

    private static String subject(String title, OrderNotice n) {
        return "%s — %s".formatted(title, n.reference());
    }

    private static String detail(OrderNotice n) {
        return n.detail() == null || n.detail().isBlank() ? "" : "\n" + n.detail() + "\n";
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAsync
    static class AsyncConfig {
    }
}
