package com.auction.server.model.item;

import com.auction.common.enums.Categories;
import com.auction.common.payload.ArtRequest;
import com.auction.common.payload.ArtResponse;
import com.auction.common.payload.ItemRequest;
import com.auction.server.model.user.User;
import com.auction.server.repository.ItemFactory;
import org.springframework.stereotype.Component;

@Component("ART")
public class ArtFactory implements ItemFactory {
    @Override
    public Item createItem(ItemRequest request, String savedFileName, User seller) {
        String artist = "Unknown";
        int yearCreated = 0;
        if (request instanceof ArtRequest artRequest) {
            if (artRequest.getArtist() != null && !artRequest.getArtist().isBlank()) {
                artist = artRequest.getArtist();
            }
            yearCreated = artRequest.getYearCreated();
        }
        return new Art(
                request.getName(),
                request.getCategories(),
                request.getDescription(),
                request.getPrice(),
                request.getBidIncrement(),
                request.getStartingTime(),
                request.getEndTime(),
                savedFileName,
                seller,
                artist,
                yearCreated
        );
    }

    @Override
    public void updateItem(Item item, ItemRequest request) {
        Art artItem = (Art) item;
        artItem.setName(request.getName());
        artItem.setDescription(request.getDescription());
        artItem.setPrice(request.getPrice());
        artItem.setBidIncrement(request.getBidIncrement());
        artItem.setCategories(request.getCategories());
        artItem.setStartingTime(request.getStartingTime());
        artItem.setEndTime(request.getEndTime());
        if (request instanceof ArtRequest artRequest) {
            if (artRequest.getArtist() != null) {
                artItem.setArtist(artRequest.getArtist());
            }
            artItem.setYearCreated(artRequest.getYearCreated());
        }
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
        artResponse.setStartingTime(artItem.getStartingTime());
        artResponse.setEndTime(artItem.getEndTime());
        artResponse.setCategories((Categories) artItem.getCategories());
        if (artItem.getSeller() != null) {
            artResponse.setSellerId(artItem.getSeller().getId());
        }
        if (artItem.getImageUrls() != null && !artItem.getImageUrls().isBlank()) {
            artResponse.setImageUrls(
                    java.util.Arrays.stream(artItem.getImageUrls().split(","))
                            .filter(s -> !s.isBlank())
                            .map(s -> "/uploads/items/" + s)
                            .toList()
            );
        }
        artResponse.setArtist(artItem.getArtist());
        artResponse.setYearCreated(artItem.getYearCreated());
        return artResponse;
    }
}
