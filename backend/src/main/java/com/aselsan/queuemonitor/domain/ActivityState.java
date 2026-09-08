package com.aselsan.queuemonitor.domain;

public enum ActivityState {

    /**
     * Worker oluşturuldu ancak çalışma döngüsüne henüz başlamadı.
     */
    STARTING,

    /**
     * Sender yeni bir mesaj üretiyor veya mesajı kuyruğa ekliyor.
     */
    PRODUCING,

    /**
     * Receiver kuyruktan bir mesaj alıyor veya alınan mesajı işliyor.
     */
    CONSUMING,

    /**
     * Kuyruk dolu olduğu için Sender yeni mesaj ekleyemiyor.
     */
    QUEUE_FULL,

    /**
     * Kuyruk boş olduğu için Receiver tüketilecek mesaj bulamıyor.
     */
    QUEUE_EMPTY,

    /**
     * Worker bir sonraki işlemine kadar belirlenen süre boyunca bekliyor.
     */
    SLEEPING,

    /**
     * Worker'ın çalışması güvenli bir şekilde sonlandırıldı.
     */
    STOPPED,

    /**
     * Worker beklenmeyen bir hata nedeniyle çalışmasını durdurdu.
     */
    FAILED
}
