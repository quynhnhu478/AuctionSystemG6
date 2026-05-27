package com.auction.server.model.item;

import com.auction.common.enums.Categories;
import com.auction.common.payload.ItemRequest;
import com.auction.common.payload.VehicleRequest;
import com.auction.common.payload.VehicleResponse;
import com.auction.server.model.user.User;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@Component("VEHICLE")
public class VehicleFactory implements ItemFactory<VehicleRequest> {
    @Override
    public Item createItem(ItemRequest itemRequest, String savedFileName, User seller){
         VehicleRequest vehicleRequest = (VehicleRequest) itemRequest;
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
        vehicleResponse.setCategories(vehicle.getCategories());
        vehicleResponse.setDescription(vehicle.getDescription());
        vehicleResponse.setPrice(vehicle.getPrice());
        vehicleResponse.setBidIncrement(vehicle.getBidIncrement());
        vehicleResponse.setSellerId(vehicle.getSeller().getId());
    }

    @Override
    public VehicleResponse mapToResponse(Item item){
        Vehicle vehicle = (Vehicle) item;
        VehicleResponse vehicleResponse = new VehicleResponse();
        vehicleResponse.setId(vehicle.getId());
        vehicleResponse.setName(item.getName());
        vehicleResponse.setCategories(item.getCategories());
        vehicleResponse.setDescription(item.getDescription());
        vehicleResponse.setPrice(item.getPrice());
        vehicleResponse.setBidIncrement(item.getBidIncrement());
        vehicleResponse.setSellerId(item.getSeller().getId());
        vehicleResponse.setStartingTime(item.getStartingTime());
        vehicleResponse.setEndTime(item.getEndTime());
        vehicleResponse.setSavedFileName(item.getImageUrl());
        return vehicleResponse;
    }
}
