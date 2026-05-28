package com.auction.common.BanMoi;


import com.auction.common.payload.UserResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BidResult {
        private UserResponse userBalance;
        private BidResponse roomUpdate;
}

