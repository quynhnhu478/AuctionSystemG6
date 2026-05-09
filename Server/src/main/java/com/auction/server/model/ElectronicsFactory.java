package com.auction.server.model;

import com.auction.server.payload.ElectronicsRequest;
import com.auction.server.payload.ElectronicsResponse;
import com.auction.server.payload.ItemResponse;

public class ElectronicsFactory implements ItemFactory<ElectronicsRequest>{
    @Override
    public Item createItem(ElectronicsRequest electronicsRequest, User seller){
        return new Electronics(
                electronicsRequest.getName(),
                electronicsRequest.getCategories(),
                electronicsRequest.getDescription(),
                electronicsRequest.getPrice(),
                seller,
                electronicsRequest.getBrand(),
                electronicsRequest.getWarrantyPeriod());
    }

    @Override
    public void updateItem(Item item, ElectronicsRequest electronicsRequest){
        Electronics electronicsItem = (Electronics) item;
        electronicsItem.setName(electronicsRequest.getName());
        electronicsItem.setDescription(electronicsRequest.getDescription());
        electronicsItem.setPrice(electronicsRequest.getPrice());
        electronicsItem.setCategories(electronicsRequest.getCategories());
        electronicsItem.setBrand(electronicsRequest.getBrand());
        electronicsItem.setWarrantyPeriod(electronicsRequest.getWarrantyPeriod());
    }

    @Override
    public ItemResponse mapToResponse(Item item){
        Electronics electronicsItem = (Electronics) item;
        ElectronicsResponse electronicsResponse = new ElectronicsResponse();
        electronicsResponse.setId(electronicsItem.getId());
        electronicsResponse.setName(electronicsItem.getName());
        electronicsResponse.setDescription(electronicsItem.getDescription());
        electronicsResponse.setPrice(electronicsItem.getPrice());
        electronicsResponse.setCategories(electronicsItem.getCategories());
        electronicsResponse.setSellerId(electronicsItem.getSeller().getId());
        electronicsResponse.setBrand(electronicsItem.getBrand());
        electronicsResponse.setWarrantyPeriod(electronicsItem.getWarrantyPeriod());
        return electronicsResponse;
    }
}
