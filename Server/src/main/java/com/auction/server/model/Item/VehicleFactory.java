package com.auction.server.model.Item;

import com.auction.common.payload.VehicleRequest;
import com.auction.common.payload.VehicleResponse;
import com.auction.server.model.User.User;
import org.springframework.stereotype.Component;

@Component("VEHICLE")
public class VehicleFactory implements ItemFactory<VehicleRequest> {
    @Override
    public Item createItem(VehicleRequest vehicleRequest, User seller){
        return new Vehicle(
                vehicleRequest.getName(),
                vehicleRequest.getCategories(),
                vehicleRequest.getDescription(),
                vehicleRequest.getPrice(),
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
        return vehicleResponse;
    }
}
