package com.workit.domain.review.dto.request;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.workit.domain.review.vo.ReviewAtmosphere;
import org.springframework.web.multipart.MultipartFile;

public class ReviewUpdateRequestDTO {

    private Integer rating;
    private String content;
    private ReviewAtmosphere atmosphere;
    private String imageUrl;
    private MultipartFile image;
    private boolean ratingProvided;
    private boolean contentProvided;
    private boolean atmosphereProvided;
    private boolean imageUrlProvided;

    public Integer getRating() {
        return rating;
    }

    @JsonSetter("rating")
    public void setRating(Integer rating) {
        this.rating = rating;
        this.ratingProvided = true;
    }

    public String getContent() {
        return content;
    }

    @JsonSetter("content")
    public void setContent(String content) {
        this.content = content;
        this.contentProvided = true;
    }

    public ReviewAtmosphere getAtmosphere() {
        return atmosphere;
    }

    @JsonSetter("atmosphere")
    public void setAtmosphere(ReviewAtmosphere atmosphere) {
        this.atmosphere = atmosphere;
        this.atmosphereProvided = true;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    @JsonSetter("imageUrl")
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
        this.imageUrlProvided = true;
    }

    public boolean isRatingProvided() {
        return ratingProvided;
    }

    public boolean isContentProvided() {
        return contentProvided;
    }

    public boolean isAtmosphereProvided() {
        return atmosphereProvided;
    }

    public boolean isImageUrlProvided() {
        return imageUrlProvided;
    }

    public boolean hasAnyField() {
        return ratingProvided || contentProvided || atmosphereProvided || imageUrlProvided
                || (image != null && !image.isEmpty());
    }

    public MultipartFile getImage() {
        return image;
    }

    public void setImage(MultipartFile image) {
        this.image = image;
    }
}
