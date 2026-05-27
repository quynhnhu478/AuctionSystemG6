package com.auction.common.payload;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BidResult {
        private UserResponse userBalance;
        private BidResponsePayload roomUpdate;
}

