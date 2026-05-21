package com.auction.common.payload;

public class HandleSellerRegistrationRequest {
    private Long registrationId;
    private String adminAction; // APPROVE, REJECT

    public HandleSellerRegistrationRequest(){}
    public HandleSellerRegistrationRequest(Long registrationId, String adminAction) {
        this.registrationId = registrationId;
        this.adminAction = adminAction;
    }
    public Long getRegistrationId() {
        return registrationId;
    }
    public void setRegistrationId(Long registrationId) {
        this.registrationId = registrationId;
    }
    public String getAdminAction() {
        return adminAction;
    }
    public void setAdminAction(String adminAction) {
        this.adminAction = adminAction;
    }
}
