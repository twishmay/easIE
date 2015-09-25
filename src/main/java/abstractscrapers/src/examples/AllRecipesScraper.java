package abstractscrapers.src.examples;

import abstractscrapers.src.Field;
import abstractscrapers.src.FieldType;
import abstractscrapers.src.SelectorType;
import abstractscrapers.src.Scrapers.BunchScraper;
import abstractscrapers.src.OutputFormatter.Snippet;
import abstractscrapers.src.Scrapers.PaginationIterator;
import abstractscrapers.src.Scrapers.StaticHTMLScraper;
import com.mongodb.BasicDBList;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import org.json.JSONArray;

/**
 *
 * @author vasgat
 */
public class AllRecipesScraper {
   private List<Field> RecipeFields;
   private List<Field> linkFields;
   
   public AllRecipesScraper(){      
      RecipeFields = new ArrayList<Field>();
      Field directions = new Field(
              "Directions",
              ".directLeft > ol > li",
              SelectorType.rawtext,
              SelectorType.CSS,
              FieldType.text,
              FieldType.list
      );
      Field rating = new Field(
              "Rating",
              ".detail-right > div:nth-child(4) > div:nth-child(1) > div:nth-child(2) > meta",
              SelectorType.rawtext,
              SelectorType.CSS,
              FieldType.text,
              "content"
      );
      Field recipeId = new Field(
              "id",
              "#zoneRecipe",
              SelectorType.rawtext,
              SelectorType.CSS,
              FieldType.text,
              "data-typespecificid"
      );
      Field Ingredients = new Field(
              "Ingredients",
              "ul.ingredient-wrap > li",
              SelectorType.rawtext,
              SelectorType.CSS,
              FieldType.text,
              FieldType.list
      );
      Field Title = new Field(
              "Recipe Title",
              "#itemTitle",
              SelectorType.rawtext,
              SelectorType.CSS,
              FieldType.text,
              FieldType.text
      );      
      RecipeFields.add(directions);
      RecipeFields.add(rating);
      RecipeFields.add(Ingredients);
      RecipeFields.add(Title);
      RecipeFields.add(recipeId);
      
      linkFields = new ArrayList<Field>();
      Field RecipeLinkField = new Field(
              "RecipeLink",
              "p > a",
              SelectorType.rawtext,
              SelectorType.CSS,
              FieldType.text,
              FieldType.link
      );
      linkFields.add(RecipeLinkField);
   }

   public BasicDBList scrapeRecipesByIngedient(List<String> ingredients) 
                              throws URISyntaxException, IOException, Exception
   {      
      HashSet<String> RecipeLinks = new HashSet();
      for (int i=0; i<ingredients.size(); i++){
         RecipeLinks.addAll(getRecipeLinksByIngredient(ingredients.get(i)));
      }
      
      BunchScraper bunchScraper = new BunchScraper(RecipeLinks,"http://allrecipes.com");
      
      
      BasicDBList list = new BasicDBList();
      ArrayList<HashMap> scrapedData = bunchScraper.scrapeFields(RecipeFields);
      for (int i=0; i<scrapedData.size(); i++){
          Snippet snippet = new Snippet(bunchScraper.scrapeFields(RecipeFields).get(i));
          list.add(snippet.getSnippetDBObject());
      }
      return list;
   }
   
   public HashSet getRecipeLinksByIngredient(String ingredient) 
                  throws URISyntaxException, IOException, Exception
   {
      HashSet<String> RecipeLinks = new HashSet();
      PaginationIterator PageIterator = new PaginationIterator(
              new StaticHTMLScraper(
                      "http://allrecipes.com",
                      "/search/default.aspx?ms=0&origin=Recipe+Search+Results&rt=r&qt=i&pqt=i&fo=0&w0="+ingredient
              ),
              "#ctl00_CenterColumnPlaceHolder_ucPager_corePager_pageNumbers > a:nth-child(9)"
      );
      ArrayList<HashMap> recipeLinks 
                  = PageIterator.scrapeTable(".recipe-info", linkFields);
      for (int j=0; j<recipeLinks.size(); j++){
         RecipeLinks.addAll(recipeLinks.get(j).values());
      }
      return RecipeLinks;
   }
   
   public List<String> readIngredientsFile(String filePath) throws IOException{
      List<String> ingredients = new ArrayList<String>();
      BufferedReader in = new BufferedReader(new FileReader(filePath));
      String line;
      while((line = in.readLine()) != null)
      {
         String[] lineFields = line.split("\\t");
         ingredients.add(lineFields[1]);
      }
      in.close();
      ingredients.remove("ingredient name");
      return ingredients;
     }
   
   public static void main(String[] args) throws IOException, Exception{ 
      AllRecipesScraper allrecipesScraper = new AllRecipesScraper();
      List<String> ingredients = allrecipesScraper.readIngredientsFile("src//main//java//abstractscrapers//src//data//ingredients.tsv");
      PrintWriter writer = new PrintWriter("recipes.json");
      writer.println(allrecipesScraper.scrapeRecipesByIngedient(ingredients));
      writer.close();
   }
}