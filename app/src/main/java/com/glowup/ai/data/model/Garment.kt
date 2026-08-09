package com.glowup.ai.data.model

enum class GarmentCategory(val displayName: String, val apiValue: String) {
    TOPS("Tops", "upper_body"),
    BOTTOMS("Bottoms", "lower_body"),
    DRESSES("Dresses", "one_piece"),
    SHOES("Shoes", "shoes")
}

enum class Gender { MEN, WOMEN }

data class Garment(
    val id: String,
    val name: String,
    val category: GarmentCategory,
    val imageUrl: String,
    val gender: Gender = Gender.WOMEN,
    val color: String = ""
)

/**
 * Optimized garment catalog for production.
 */
object GarmentCatalog {
    val garments: List<Garment> = listOf(
        // MEN - Tops (2 Shirts, 2 T-Shirts)
        Garment("m_t1", "Formal White Shirt", GarmentCategory.TOPS, "https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?w=400&h=600&fit=crop", Gender.MEN),
        Garment("m_t2", "Casual Blue Shirt", GarmentCategory.TOPS, "https://images.unsplash.com/photo-1594938298603-c8148c4dae35?w=400&h=600&fit=crop", Gender.MEN),
        Garment("m_t4", "Graphic Summer Tee", GarmentCategory.TOPS, "https://images.unsplash.com/photo-1583743814966-8936f5b7be1a?w=400&h=600&fit=crop", Gender.MEN),
        
        // MEN - Bottoms (2 Jeans, 2 Half Pants)
        Garment("m_b1", "Classic Blue Jeans", GarmentCategory.BOTTOMS, "https://images.unsplash.com/photo-1542272604-787c3835535d?w=400&h=600&fit=crop", Gender.MEN),
        Garment("m_b2", "Slim Black Jeans", GarmentCategory.BOTTOMS, "https://images.unsplash.com/photo-1473966968600-fa801b869a1a?w=400&h=600&fit=crop", Gender.MEN),
        Garment("m_b4", "Cargo Half Pants", GarmentCategory.BOTTOMS, "https://images.unsplash.com/photo-1591195853828-11db59a44f6b?w=400&h=600&fit=crop", Gender.MEN),
        
        // WOMEN - Dresses (4 Dresses)
        Garment("w_d1", "Floral Summer Dress", GarmentCategory.DRESSES, "https://images.unsplash.com/photo-1572804013309-59a88b7e92f1?w=400&h=600&fit=crop", Gender.WOMEN),
        Garment("w_d2", "Classic Black Dress", GarmentCategory.DRESSES, "https://images.unsplash.com/photo-1595777457583-95e059d581b8?w=400&h=600&fit=crop", Gender.WOMEN),
        Garment("w_d4", "Bohemian Maxi Dress", GarmentCategory.DRESSES, "https://images.unsplash.com/photo-1566174053879-31528523f8ae?w=400&h=600&fit=crop", Gender.WOMEN),
        
        // SHOES (1 Men, 1 Women)
        Garment("m_s1", "Urban Sneakers", GarmentCategory.SHOES, "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=400&h=600&fit=crop", Gender.MEN),
        Garment("w_s1", "Designer Heels", GarmentCategory.SHOES, "https://images.unsplash.com/photo-1543163521-1bf539c55dd2?w=400&h=600&fit=crop", Gender.WOMEN)
    )

    fun getByCategoryAndGender(category: GarmentCategory, gender: Gender): List<Garment> =
        garments.filter { it.category == category && it.gender == gender }
}
