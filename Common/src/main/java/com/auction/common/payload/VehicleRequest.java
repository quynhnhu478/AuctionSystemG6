<<<<<<<< HEAD:Server/src/main/java/com/auction/server/payload/Item/VehicleRequest.java
package com.auction.server.payload.Item;

import com.auction.server.model.Item.Categories;
========
package com.auction.common.payload;

import com.auction.common.enums.Categories;
>>>>>>>> origin/ngọc_2:Common/src/main/java/com/auction/common/payload/VehicleRequest.java

public class VehicleRequest extends ItemRequest {
    public VehicleRequest() {
        super();
    }
    public VehicleRequest(String name, String description, Double price, Enum<Categories> categories, Long sellerId) {
        super(name, description, price, categories, sellerId);
    }
}
