package com.auction.server.model.item;

import com.auction.common.enums.Categories;
import com.auction.common.payload.ElectronicsRequest;
import com.auction.common.payload.ElectronicsResponse;
import com.auction.common.payload.ItemRequest;
import com.auction.server.model.user.User;
import org.springframework.stereotype.Component;

@Component("ELECTRONICS")
public class ElectronicsFactory implements ItemFactory {
    @Override
    public Item createItem(ItemRequest request, String savedFileName, User seller) {
        String brand = null;
        String warrantyPeriod = null;
        if (request instanceof ElectronicsRequest electronicsRequest) {
            brand = electronicsRequest.getBrand();
            warrantyPeriod = electronicsRequest.getWarrantyPeriod();
        }
        return new Electronics(
                request.getName(),
                request.getCategories(),
                request.getDescription(),
                request.getPrice(),
                request.getBidIncrement(),
                request.getStartingTime(),
                request.getEndTime(),
                savedFileName,
                seller,
                brand,
                warrantyPeriod
        );
    }

    @Override
    public void updateItem(Item item, ItemRequest request) {
        Electronics electronicsItem = (Electronics) item;
        electronicsItem.setName(request.getName());
        electronicsItem.setDescription(request.getDescription());
        electronicsItem.setPrice(request.getPrice());
        electronicsItem.setBidIncrement(request.getBidIncrement());
        electronicsItem.setCategories(request.getCategories());
        electronicsItem.setStartingTime(request.getStartingTime());
        electronicsItem.setEndTime(request.getEndTime());
        if (request instanceof ElectronicsRequest electronicsRequest) {
            electronicsItem.setBrand(electronicsRequest.getBrand());
            electronicsItem.setWarrantyPeriod(electronicsRequest.getWarrantyPeriod());
        }
    }

    @Override
    public ElectronicsResponse mapToResponse(Item item) {
        Electronics electronicsItem = (Electronics) item;
        ElectronicsResponse electronicsResponse = new ElectronicsResponse();
        electronicsResponse.setId(electronicsItem.getId());
        electronicsResponse.setName(electronicsItem.getName());
        electronicsResponse.setDescription(electronicsItem.getDescription());
        electronicsResponse.setPrice(electronicsItem.getPrice());
        electronicsResponse.setBidIncrement(electronicsItem.getBidIncrement());
        electronicsResponse.setStartingTime(electronicsItem.getStartingTime());
        electronicsResponse.setEndTime(electronicsItem.getEndTime());
        electronicsResponse.setCategories((Categories) electronicsItem.getCategories());
        if (electronicsItem.getSeller() != null) {
            electronicsResponse.setSellerId(electronicsItem.getSeller().getId());
        }
        if (electronicsItem.getImageUrl() != null && !electronicsItem.getImageUrl().isBlank()) {
            electronicsResponse.setImageUrl(electronicsItem.getImageUrl());
        }
        if (electronicsItem.getImageUrls() != null && !electronicsItem.getImageUrls().isBlank()) {
            electronicsResponse.setImageUrls(
                    java.util.Arrays.stream(electronicsItem.getImageUrls().split(","))
                            .filter(s -> !s.isBlank())
                            .toList()
            );
        } else if (electronicsResponse.getImageUrl() != null && !electronicsResponse.getImageUrl().isBlank()) {
            electronicsResponse.setImageUrls(java.util.List.of(electronicsResponse.getImageUrl()));
        }
        electronicsResponse.setBrand(electronicsItem.getBrand());
        electronicsResponse.setWarrantyPeriod(electronicsItem.getWarrantyPeriod());
        return electronicsResponse;
    }
}
