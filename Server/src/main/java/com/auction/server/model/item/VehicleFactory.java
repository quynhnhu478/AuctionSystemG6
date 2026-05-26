package com.auction.server.model.item;

import com.auction.common.enums.Categories;
import com.auction.common.payload.VehicleRequest;
import com.auction.common.payload.VehicleResponse;
import com.auction.server.model.user.User;
import com.auction.server.repository.ItemFactory;
import org.springframework.stereotype.Component;

@Component("VEHICLE")
public class VehicleFactory implements ItemFactory<VehicleRequest> {
    @Override
    public Item createItem(VehicleRequest vehicleRequest, String savedFileName, User seller){
        return new Vehicle(
                vehicleRequest.getName(),
                vehicleRequest.getCategories(),
                vehicleRequest.getDescription(),
                vehicleRequest.getPrice(),
                vehicleRequest.getBidIncrement(),
                vehicleRequest.getStartingTime(),
                vehicleRequest.getEndTime(),
                savedFileName,
                seller
        );
    }

    @Override
    public void updateItem(Item item, VehicleRequest vehicleRequest){
        Vehicle vehicle = (Vehicle) item;
        VehicleResponse vehicleResponse = new VehicleResponse();
        vehicleResponse.setName(vehicle.getName());
        vehicleResponse.setCategories((Categories) vehicle.getCategories());
        vehicleResponse.setDescription(vehicle.getDescription());
        vehicleResponse.setPrice(vehicle.getPrice());
        vehicleResponse.setBidIncrement(vehicle.getBidIncrement());
    }

    @Override
    public VehicleResponse mapToResponse(Item item){
        Vehicle vehicle = (Vehicle) item;
        VehicleResponse vehicleResponse = new VehicleResponse();
        vehicleResponse.setId(vehicle.getId());
        vehicleResponse.setName(item.getName());
        vehicleResponse.setCategories((Categories) item.getCategories());
        vehicleResponse.setDescription(item.getDescription());
        vehicleResponse.setPrice(item.getPrice());
        vehicleResponse.setBidIncrement(item.getBidIncrement());
        vehicleResponse.setSellerId(vehicle.getSeller().getId());
        vehicleResponse.setImageUrl(vehicle.getImageUrl());
        return vehicleResponse;
    }
}
