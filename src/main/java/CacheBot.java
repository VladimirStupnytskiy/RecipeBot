import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.ArrayList;

public class CacheBot {
    private Recipe recipe;
    private String userID;
    private String chatID;
    private int messageID;
    private Message message;
    private String user;
    private String subMenuState = "";
    private String menuState = "";
    private String callData = "";
    private String edidedTitle = "";
    private Recipe purcasedRecipe;
    private String[] purchasedComponents;
    private String currentRecipeShowedTitled = "";
    private boolean[] clicked;
    private Recipe importedRecipe;
    private ArrayList<String> recipeTitles;

    public Recipe getRecipe() {
        return recipe;
    }

    public void setRecipe(Recipe recipe) {
        this.recipe = recipe;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getChatID() {
        return chatID;
    }

    public void setChatID(String chatID) {
        this.chatID = chatID;
    }

    public int getMessageID() {
        return messageID;
    }

    public void setMessageID(int messageID) {
        this.messageID = messageID;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getSubMenuState() {
        return subMenuState;
    }

    public void setSubMenuState(String subMenuState) {
        this.subMenuState = subMenuState;
    }

    public String getMenuState() {
        return menuState;
    }

    public void setMenuState(String menuState) {
        this.menuState = menuState;
    }

    public String getCallData() {
        return callData;
    }

    public void setCallData(String callData) {
        this.callData = callData;
    }

    public String getEdidedTitle() {
        return edidedTitle;
    }

    public void setEdidedTitle(String edidedTitle) {
        this.edidedTitle = edidedTitle;
    }

    public Recipe getPurcasedRecipe() {
        return purcasedRecipe;
    }

    public void setPurcasedRecipe(Recipe purcasedRecipe) {
        this.purcasedRecipe = purcasedRecipe;
    }

    public String[] getPurchasedComponents() {
        return purchasedComponents;
    }

    public void setPurchasedComponents(String[] purchasedComponents) {
        this.purchasedComponents = purchasedComponents;
    }

    public String getCurrentRecipeShowedTitled() {
        return currentRecipeShowedTitled;
    }

    public void setCurrentRecipeShowedTitled(String currentRecipeShowedTitled) {
        this.currentRecipeShowedTitled = currentRecipeShowedTitled;
    }

    public boolean[] getClicked() {
        return clicked;
    }

    public void setClicked(boolean[] clicked) {
        this.clicked = clicked;
    }

    public Recipe getImportedRecipe() {
        return importedRecipe;
    }

    public void setImportedRecipe(Recipe importedRecipe) {
        this.importedRecipe = importedRecipe;
    }

    public ArrayList<String> getRecipeTitles() {
        return recipeTitles;
    }

    public void setRecipeTitles(ArrayList<String> recipeTitles) {
        this.recipeTitles = recipeTitles;
    }
}
