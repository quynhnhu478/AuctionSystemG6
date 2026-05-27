package com.auction.server.model.item;

import com.auction.common.enums.Categories;
import com.auction.common.payload.ItemRequest;
import com.auction.common.payload.VehicleResponse;
import com.auction.server.model.user.User;
import com.auction.server.repository.ItemFactory;
import org.springframework.stereotype.Component;

@Component("VEHICLE")
public class VehicleFactory implements ItemFactory {
    @Override
    public Item createItem(ItemRequest request, String savedFileName, User seller) {
        return new Vehicle(
                request.getName(),
                request.getCategories(),
                request.getDescription(),
                request.getPrice(),
                request.getBidIncrement(),
                request.getStartingTime(),
                request.getEndTime(),
                savedFileName,
                seller
        );
    }

    @Override
    public void updateItem(Item item, ItemRequest request) {
        Vehicle vehicle = (Vehicle) item;
        vehicle.setName(request.getName());
        vehicle.setDescription(request.getDescription());
        vehicle.setPrice(request.getPrice());
        vehicle.setBidIncrement(request.getBidIncrement());
        vehicle.setCategories(request.getCategories());
        vehicle.setStartingTime(request.getStartingTime());
        vehicle.setEndTime(request.getEndTime());
    }

    @Override
    public VehicleResponse mapToResponse(Item item) {
        Vehicle vehicle = (Vehicle) item;
        VehicleResponse response = new VehicleResponse();
        response.setId(vehicle.getId());
        response.setName(vehicle.getName());
        response.setDescription(vehicle.getDescription());
        response.setPrice(vehicle.getPrice());
        response.setBidIncrement(vehicle.getBidIncrement());
        response.setStartingTime(vehicle.getStartingTime());
        response.setEndTime(vehicle.getEndTime());
        response.setCategories((Categories) vehicle.getCategories());
        if (vehicle.getSeller() != null) {
            response.setSellerId(vehicle.getSeller().getId());
        }
        if (vehicle.getImageUrl() != null && !vehicle.getImageUrl().isBlank()) {
            response.setImageUrl("/uploads/items/" + vehicle.getImageUrl());
        }
        return response;
    }
}
