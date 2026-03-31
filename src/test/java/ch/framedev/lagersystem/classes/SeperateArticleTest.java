package ch.framedev.lagersystem.classes;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeperateArticleTest {

    @Test
    @DisplayName("constructor and setter: expose stored values")
    void constructorAndSetter_exposeStoredValues() {
        SeperateArticle article = new SeperateArticle(1, "1001", "Blau");

        article.setIndex(5);

        assertEquals(5, article.getIndex());
        assertEquals("1001", article.getArticleNumber());
        assertEquals("Blau", article.getOtherDetails());
    }

    @Test
    @DisplayName("toJson/fromJson: roundtrip keeps all fields")
    void toJsonFromJson_roundtripKeepsFields() {
        SeperateArticle original = new SeperateArticle(7, "2205", "XL");

        String json = original.toJson();
        SeperateArticle restored = SeperateArticle.fromJson(json);

        assertTrue(json.contains("\"index\": 7"));
        assertTrue(json.contains("\"articleNumber\": \"2205\""));
        assertTrue(json.contains("\"otherDetails\": \"XL\""));
        assertEquals(7, restored.getIndex());
        assertEquals("2205", restored.getArticleNumber());
        assertEquals("XL", restored.getOtherDetails());
    }

    @Test
    @DisplayName("fromJsonObject: creates instance from JsonObject")
    void fromJsonObject_createsInstance() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("index", 3);
        jsonObject.addProperty("articleNumber", "1101");
        jsonObject.addProperty("otherDetails", "Gruen");

        SeperateArticle article = SeperateArticle.fromJsonObject(jsonObject);

        assertEquals(3, article.getIndex());
        assertEquals("1101", article.getArticleNumber());
        assertEquals("Gruen", article.getOtherDetails());
    }
}
