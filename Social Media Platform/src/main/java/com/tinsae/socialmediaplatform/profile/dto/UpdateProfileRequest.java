package com.tinsae.socialmediaplatform.profile.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 100, message = "Display name must not exceed 100 characters.")
        String displayName,

        @Size(max = 500, message = "Bio must not exceed 500 characters.")
        String bio,

        @Size(max = 500, message = "Avatar URL must not exceed 500 characters.")
        String avatarUrl,

        @Size(max = 500, message = "Cover image URL must not exceed 500 characters.")
        String coverImageUrl,

        @Size(max = 255, message = "Street must not exceed 255 characters.")
        String street,

        @Size(max = 120, message = "City must not exceed 120 characters.")
        String city,

        @Size(max = 120, message = "State must not exceed 120 characters.")
        String state,

        @Size(max = 120, message = "Country must not exceed 120 characters.")
        String country,

        @Size(max = 255, message = "Website URL must not exceed 255 characters.")
        String websiteUrl
) {
}
