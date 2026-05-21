package com.auction.server.model.Item;


import com.auction.common.payload.ArtRequest;
import com.auction.common.payload.ArtResponse;

import com.auction.server.model.User.User;
import org.springframework.stereotype.Component;

@Component("Art")
public class ArtFactory implements ItemFactory<ArtRequest> {
    @Override
    public Item createItem(ArtRequest artRequest, User seller) {
        return new Art(
                artRequest.getName(),
                artRequest.getCategories(),
                artRequest.getDescription(),
                artRequest.getPrice(),
                artRequest.getBidIncrement(),
                artRequest.getStartingTime(),
                artRequest.getEndTime(),
                seller,
                artRequest.getArtist(),
                artRequest.getYearCreated()
        );
    }

    @Override
    public void updateItem(Item item, ArtRequest artRequest) {
        Art artItem = (Art) item;
        artItem.setName(artRequest.getName());
        artItem.setDescription(artRequest.getDescription());
        artItem.setPrice(artRequest.getPrice());
        artItem.setBidIncrement(artRequest.getBidIncrement());
        artItem.setCategories(artRequest.getCategories());
        artItem.setArtist(artRequest.getArtist());
        artItem.setYearCreated(artRequest.getYearCreated());
    }
    @Override
    public ArtResponse mapToResponse(Item item) {
        Art artItem = (Art) item;
        ArtResponse artResponse = new ArtResponse();
        artResponse.setId(item.getId());
        artResponse.setName(artItem.getName());
        artResponse.setDescription(artItem.getDescription());
        artResponse.setPrice(artItem.getPrice());
        artResponse.setBidIncrement(artItem.getBidIncrement());
        artResponse.setCategories(artItem.getCategories());
        artResponse.setSellerId(artItem.getSeller().getID());
        artResponse.setArtist(artItem.getArtist());
        artResponse.setYearCreated(artItem.getYearCreated());
        return artResponse;
    }
}
