import org.checkerframework.checker.units.qual.A;
import org.checkerframework.checker.units.qual.C;
import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;

public class recipeBot extends TelegramLongPollingBot {

    Map<Long, CacheBot> map = new HashMap<Long, CacheBot>();
    DBController dbController = DBController.getInstance();
    String []serviceCommands = {"Відкрити рецепт","Додати новий рецепт", "Показати випадковий рецепт", "Інформація","/start","Перед тобою головне меню. Вибери потрібний пункт"};
    int pageNumber = 1;
    int qty = 7;

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Message message = update.getMessage();
            long chatID = message.getChatId();
            if(!map.containsKey(chatID)) map.put(chatID,new CacheBot());
            CacheBot cacheBot = map.get(chatID);
            cacheBot.setChatID(update.getMessage().getChatId().toString()); //  set chatID
            cacheBot.setMessage(message);//set message
            cacheBot.setUser(message.getFrom().
                    getFirstName().concat(" ").concat(message.getFrom().getLastName())); //set User
            //if (message.getFrom().getId() == 308764785) userID = String.valueOf(422116466);
            cacheBot.setUserID(message.getFrom().getId().toString()); // set User ID
            cacheBot.setMessageID(message.getMessageId()); //setMessageID
            String message_text = message.getText();

            if (message_text.equals("/start")) {
                MySendMassage(mainMenuKeyboard(), "Перед тобою головне меню. Вибери потрібний пункт",chatID);
            }else{
                if (message_text.equals("Відкрити рецепт")) {
                    cacheBot.setRecipeTitles(dbController.recipeList(map.get(chatID).getUserID()));

                    if (cacheBot.getRecipeTitles().size() == 0) {
                        MySendMassage(myInlineKeyboard(new LinkedHashMap() {{
                            put("Додати перший рецепт", "CREATE_RECIPE");
                            put("Головне меню", "MAIN_MENU");
                        }}, 2), "Овва \uD83D\uDE2E, в тебе ще немає доданих рецептів",chatID);
                    } else
                        MySendMassage(myInlineKeyboard(new LinkedHashMap() {{
                            put("\uD83D\uDCD2 Відкрити список", "OPEN_LIST");
                            put("\uD83D\uDD0E Пошук по назві", "SEARCH_BY_TITLE");
                        }}, 2), "Виберіть зручний спосіб", chatID);
                }else{
                    if (message_text.equals("Додати новий рецепт")) {
                        createRecipe(chatID);
                    } else{
                        if (message_text.equals("Показати випадковий рецепт")) {
                            cacheBot.setRecipeTitles(dbController.recipeList(map.get(chatID).getUserID()));
                            if (cacheBot.getRecipeTitles().size() == 0) {
                                MySendMassage(myInlineKeyboard(new LinkedHashMap() {{
                                    put("Додати перший рецепт", "CREATE_RECIPE");
                                    put("Головне меню", "MAIN_MENU");
                                }}, 2), "Овва \uD83D\uDE2E, в тебе ще немає доданих рецептів",chatID);
                            } else
                                recipeCreateFromJSON(dbController.findRecipeByTitle(cacheBot.getRecipeTitles().
                                        get(0 + (int) (Math.random() * cacheBot.getRecipeTitles().size()))),chatID);
                        } else{
                            if (message_text.equals("Інформація")) {
                                String info = "Цей бот - твоя персональна книга рецептів.\n\n" +
                                        "З його допомогою ти можеш:\n" +
                                        " - додавати cвої улюблені рецепти\n" +
                                        " - формувати зручний список інгредієнтів для покупок\n" +
                                        " - довірити боту вибрати випадкову страву\n\n" +
                                        "Тисни /start щоб розпочати \n\n" +
                                        "Для комунікації з автором: @stupnv";
                                MySendMassage(mainMenuKeyboard(), info,chatID);
                            }
                        }
                    }
                }
            }


            //*** <menu: Додати новий рецепт>
            if (cacheBot.getMenuState().equals("MENU_ADD") && !checkIfServiceCommand(message_text)) {
                switch (cacheBot.getSubMenuState()) {
                    case ("SUBMENU_TITLE"):
                        cacheBot.setRecipe(new Recipe());
                        cacheBot.getRecipe().setTitle(checkUniqTitle(message_text,chatID));
                        MySendMassage(myInlineKeyboard(new LinkedHashMap(){{
                            put("Скасувати", "EXIT_RECIPE");
                        }},1),"А тепер введіть *КОМПОНЕНТИ* страви\n_(Кожен компонент з нового рядка)_",chatID);
                        cacheBot.setSubMenuState("SUBMENU_COMPS");
                        break;

                    case ("EDIT_TITLE"):
                        cacheBot.getRecipe().setTitle(message_text);
                        showRecipe(chatID);
                        cacheBot.setSubMenuState("");
                        break;

                    case ("SUBMENU_COMPS"):
                        cacheBot.getRecipe().setComponents(message_text.split("\\R+"));
                        MySendMassage(myInlineKeyboard(new LinkedHashMap(){{
                            put("Скасувати", "EXIT_RECIPE");
                        }},1),"А тепер введіть *СПОСІБ ПРИГОТУВАННЯ* страви",chatID);

                        cacheBot.setSubMenuState("SUBMENU_PREPAR");
                        break;

                    case ("EDIT_COMPONENTS"):
                        cacheBot.getRecipe().setComponents(message_text.split("\\R+"));
                        showRecipe(chatID);
                        break;

                    case ("SUBMENU_PREPAR"):
                        cacheBot.getRecipe().setPreparing(message_text);
                        MySendMassage(myInlineKeyboard(new LinkedHashMap() {{
                            put("так", "YES_IMAGE");
                            put("ні", "NO_IMAGE");
                        }}, 2), "Додати картинку до рецепту?",chatID);
                        cacheBot.setSubMenuState("SUBMENU_ADD_PIC");
                        break;
                    case ("EDIT_PREPARATION"):
                        cacheBot.getRecipe().setPreparing(message_text);
                        showRecipe(chatID);
                        break;
                }
            }

            if (cacheBot.getMenuState().equals("EDITING_PREPARATION_DB") && !checkIfServiceCommand(message_text)) {
                dbController.updateField(cacheBot.getEdidedTitle(), "Cooking", message_text);
                recipeCreateFromJSON(dbController.findRecipeByTitle(cacheBot.getCurrentRecipeShowedTitled()),chatID);
                cacheBot.setMenuState("");
            }
            if (cacheBot.getMenuState().equals("EDITING_TITLE_DB") && !checkIfServiceCommand(message_text)) {
                dbController.updateField(cacheBot.getEdidedTitle(), "title", message_text);
                recipeCreateFromJSON(dbController.findRecipeByTitle(cacheBot.getCurrentRecipeShowedTitled()),chatID);
                cacheBot.setMenuState("");
            }
            if (cacheBot.getMenuState().equals("EDITING_COMPONENTS_DB") && !checkIfServiceCommand(message_text)) {
                dbController.updateField(cacheBot.getEdidedTitle(), "Components", message_text.split("\\R+"));
                recipeCreateFromJSON(dbController.findRecipeByTitle(cacheBot.getCurrentRecipeShowedTitled()),chatID);
                cacheBot.setMenuState("");
            }

            if (cacheBot.getSubMenuState().equals("SEARCH_BY_TITLE")) {
                if (dbController.findRecipeByTitle(message_text,true).size() == 0) {
                    MySendMassage(myInlineKeyboard(new LinkedHashMap() {{
                        put("\uD83D\uDCD2 Відкрити список", "OPEN_LIST");
                        put("\uD83D\uDD0E Пошук по назві", "SEARCH_BY_TITLE");
                    }}, 2), "Не вдалося знайти рецепт \uD83D\uDE12 \nУточни назву або пошукай в списку",chatID);
                } else
                    recipeCreateFromJSON(dbController.findRecipeByTitle(message_text,true),chatID);
                cacheBot.setSubMenuState("");
            }

        } else if (update.hasCallbackQuery()) {
            long chatID = update.getCallbackQuery().getMessage().getChatId();
            CacheBot cacheBot = map.get(chatID);
            cacheBot.setCallData(update.getCallbackQuery().getData());
            cacheBot.setMessageID(update.getCallbackQuery().getMessage().getMessageId());
            cacheBot.setMessage(update.getCallbackQuery().getMessage());


            if (cacheBot.getCallData().contains("OPEN_RECIPE")) {
                String[] strings = cacheBot.getCallData().split(";");
                recipeCreateFromJSON(dbController.findRecipeByTitle(strings[1]),chatID);
            } else {
                if (cacheBot.getCallData().contains("PURCHASE;")) {
                    String[] strings = cacheBot.getCallData().split(";");
                    editPurchaseList(Integer.parseInt(strings[1]), cacheBot.getMessageID(), chatID, update.getCallbackQuery().getId());
                }
            }

            switch (cacheBot.getCallData()) {
                case ("EXIT_RECIPE"):
                    cacheBot.setMenuState("");
                    cacheBot.setSubMenuState("");

                    if (cacheBot.getRecipe()!=null) {
                        cacheBot.getRecipe().setComponents(null);
                        cacheBot.getRecipe().setTitle("");
                        cacheBot.getRecipe().setPreparing("");
                    }
                    MySendMassage(mainMenuKeyboard(), "Перед тобою головне меню. Вибери потрібний пункт",chatID);
                    break;
                case ("CLOSE"):
                    EditMessageReplyMarkup editMessageReplyMarkup = new EditMessageReplyMarkup();
                    editMessageReplyMarkup.setMessageId(cacheBot.getMessageID());
                    editMessageReplyMarkup.setChatId(String.valueOf(chatID));
                    try {
                        execute(editMessageReplyMarkup);
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                    break;

                case ("GO_BACK_DB"):
                    editInlineMenu(cacheBot.getMessageID(), chatID, myInlineKeyboard(new LinkedHashMap() {{
                        put("\uD83D\uDED2 Cформувати список покупок", "PURCHASE_LIST");
                        put("✏️  Редагувати", "EDIT_IN_DB");
                        put("〰️ Сховати меню", "CLOSE");
                    }}, 1));
                    break;
                case ("EDIT_IN_DB"):
                    cacheBot.setMenuState("");
                    cacheBot.setCurrentRecipeShowedTitled(getTitleOfMessage(cacheBot.getMessage().getText()));
                    editInlineMenu(cacheBot.getMessageID(), chatID, myInlineKeyboard(new LinkedHashMap() {{
                        put("✏️Назву", "EDIT_TITLE_DB");
                        put("\uD83D\uDCDD Список компонентів", "EDIT_COMPONENTS_DB");
                        put("\uD83C\uDF72 Спосіб приготування", "EDIT_PREPARATION_DB");
                        put("\uD83C\uDFDE Змінити зображення", "EDIT_IMAGE_DB");
                        put("✖️Видалити рецепт", "REMOVE_FROM_DB");
                        put("\uD83D\uDD19 Повернутись назад", "GO_BACK_DB");
                    }}, 1));
                    cacheBot.setEdidedTitle(getTitleOfMessage(update.getCallbackQuery().getMessage().getText()));
                    break;
                case ("REMOVE_FROM_DB"):
                    dbController.removeRecipe(getTitleOfMessage(update.getCallbackQuery().getMessage().getText()));
                    sendServiceMessageToChat("Рецепт видалено з книги", update.getCallbackQuery().getId());
                    EditMessageReplyMarkup editMessageReplyMarkup2 = new EditMessageReplyMarkup();
                    editMessageReplyMarkup2.setMessageId(cacheBot.getMessageID());
                    editMessageReplyMarkup2.setChatId(cacheBot.getChatID());
                    try {
                        execute(editMessageReplyMarkup2);
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                    break;

                case ("EDIT_TITLE_DB"):
                    MySendMassage(myInlineKeyboard(new LinkedHashMap() {{
                        put("Скасувати", "EDIT_IN_DB");
                    }}, 1), "Введіть нову назву рецепту",chatID);

                    cacheBot.setMenuState("EDITING_TITLE_DB");
                    break;

                case ("EDIT_COMPONENTS_DB"):
                    MySendMassage(myInlineKeyboard(new LinkedHashMap() {{
                        put("Скасувати", "EDIT_IN_DB");
                    }}, 1), "Введіть нові компоненти",chatID);
                    cacheBot.setMenuState("EDITING_COMPONENTS_DB");
                    break;

                case ("EDIT_PREPARATION_DB"):

                    MySendMassage(myInlineKeyboard(new LinkedHashMap() {{
                        put("Скасувати", "EDIT_IN_DB");
                    }}, 1), "Введіть новий спосіб приготування",chatID);

                    cacheBot.setMenuState("EDITING_PREPARATION_DB");
                    break;

                case ("EDIT_IMAGE_DB"):
                    MySendMassage(myInlineKeyboard(new LinkedHashMap() {{
                        put("Скасувати", "EDIT_IN_DB");
                    }}, 1), "Додайте нову картинку в чат\uD83C\uDFDE",chatID);
                    cacheBot.setMenuState("EDITING_IMAGE_DB");
                    break;

                case ("PURCHASE_LIST"):
                    pageNumber = 1;
                    createPurchaseList(chatID);
                    break;
                case ("NO_IMAGE"):
                    cacheBot.setSubMenuState("EDITING_IMAGE_DB");
                    showRecipe(chatID);
                    //menuState = "";
                    break;
                case ("MAIN_MENU"):
                    MySendMassage(mainMenuKeyboard(), "Перед вами головне меню. Виберіть потрібний пункт",chatID);
                    break;
                case ("NEXT"):
                    pageNumber++;
                    editInlineMenu(cacheBot.getMessageID(), chatID);
                    break;
                case "BACK":
                    pageNumber--;
                    editInlineMenu(cacheBot.getMessageID(), chatID);
                    break;
                case ("YES_IMAGE"):

                    MySendMassage(myInlineKeyboard(new LinkedHashMap() {{
                        put("Скасувати", "NO_IMAGE");
                    }}, 1), "Відправте картинку в чат\uD83C\uDFDE",chatID);

                    cacheBot.setSubMenuState("ADDING_IMAGE");
                    break;
                case ("EDIT_RECIPE"):
                    cacheBot.setSubMenuState("");

                    editInlineMenu(cacheBot.getMessageID(), chatID, myInlineKeyboard(new LinkedHashMap() {{
                        put("Назву", "EDIT_TITLE");
                        put("Компоненти страви", "EDIT_COMPONENTS");
                        put("Приготування", "EDIT_PREPARATION");
                        put("Картинку", "EDIT_IMAGE");
                        put("Назад", "BACK_");
                    }}, 2));
                    break;

                case ("BACK_"):
                    editInlineMenu(cacheBot.getMessageID(), chatID, myInlineKeyboard(new LinkedHashMap() {{
                        put("\uD83D\uDCBE Зберегти в базу даних", "SAVE_TO_DB");
                        put("✏️Редагувати", "EDIT_RECIPE");
                        put("\uD83D\uDD19 Вийти", "EXIT_RECIPE");
                    }}, 1));

                    break;
                case ("DELETE_MSG"):
                    deleteMessage(cacheBot.getMessageID(), chatID);
                    cacheBot.setSubMenuState("");
                    break;

                case ("EDIT_TITLE"):
                    MySendMassage(myInlineKeyboard(new LinkedHashMap() {{
                        put("Скасувати", "DELETE_MSG");
                    }}, 1), "Введіть нову назву",chatID);
                    cacheBot.setMenuState("MENU_ADD");
                    cacheBot.setSubMenuState("EDIT_TITLE");
                    break;
                case ("EDIT_COMPONENTS"):
                    MySendMassage(myInlineKeyboard(new LinkedHashMap() {{
                        put("Скасувати", "DELETE_MSG");
                    }}, 1), "Введіть новий список компонентів",chatID);
                    cacheBot.setMenuState("MENU_ADD");
                    cacheBot.setSubMenuState("EDIT_COMPONENTS");
                    break;
                case ("EDIT_PREPARATION"):
                    MySendMassage(myInlineKeyboard(new LinkedHashMap() {{
                        put("Скасувати", "DELETE_MSG");
                    }}, 1), "Введіть новий спосіб приготування",chatID);
                    cacheBot.setMenuState("MENU_ADD");
                    cacheBot.setSubMenuState("EDIT_PREPARATION");
                    break;
                case ("EDIT_IMAGE"):
                    MySendMassage(myInlineKeyboard(new LinkedHashMap() {{
                        put("Скасувати", "DELETE_MSG");
                    }}, 1), "Відправте нову картинку в чат",chatID);
                    cacheBot.setSubMenuState("ADDING_IMAGE");
                    break;
                case ("CREATE_RECIPE"):
                    createRecipe(chatID);
                    break;
                case ("SAVE_TO_DB"):
                    dbController.addRecipe(cacheBot.getUserID(), cacheBot.getRecipe().getTitle(), cacheBot.getRecipe().getComponents(), cacheBot.getRecipe().getPreparing(), cacheBot.getRecipe().getImage(),cacheBot.getUser());
                    sendServiceMessageToChat("Рецепт додано до книги", update.getCallbackQuery().getId());
                    cacheBot.setSubMenuState("");
                    cacheBot.setMenuState("");
                    break;
                case ("OPEN_LIST"):
                    pageNumber = 1;
                    MySendMassage(myInlineKeyboard(1,chatID), "Список рецептів:",chatID);
                    break;
                case ("SEARCH_BY_TITLE"):
                    cacheBot.setSubMenuState("SEARCH_BY_TITLE");
                    MySendMassage("Введіть назву рецепта для пошуку",chatID);
                    break;
            }
        } else if (update.getMessage().hasPhoto() && map.get(update.getMessage().getChatId()).getSubMenuState().equals("ADDING_IMAGE")) {
            CacheBot cacheBot = map.get(update.getMessage().getChatId());
            cacheBot.setMessage(update.getMessage());
            long chatId = update.getMessage().getChatId();
            List<PhotoSize> photos = cacheBot.getMessage().getPhoto();
            PhotoSize photo = photos.get(photos.size() - 2);
            cacheBot.getRecipe().setImage(photo.getFileId().getBytes());
            showRecipe(chatId);
            cacheBot.setSubMenuState("");
        } else if (update.getMessage().hasPhoto() && map.get(update.getMessage().getChatId()).getMenuState().equals("EDITING_IMAGE_DB")) {
            CacheBot cacheBot = map.get(update.getMessage().getChatId());
            cacheBot.setMessage(update.getMessage());
            long chatId = update.getMessage().getChatId();
            List<PhotoSize> photos = cacheBot.getMessage().getPhoto();
            PhotoSize photo = photos.get(photos.size() - 2);
            DBController.updateField(cacheBot.getEdidedTitle(), "photo", photo.getFileId().getBytes());
            recipeCreateFromJSON(dbController.findRecipeByTitle(cacheBot.getCurrentRecipeShowedTitled()),chatId);
            cacheBot.setMenuState("");
        }
    }

    private void createRecipe(long chatID) {
        MySendMassage(myInlineKeyboard(new LinkedHashMap(){{
            put("Скасувати", "EXIT_RECIPE");
        }},1),"Додаємо новий рецепт. \nВведіть *НАЗВУ* страви",chatID);
        map.get(chatID).setSubMenuState("SUBMENU_TITLE");
        map.get(chatID).setMenuState("MENU_ADD");
    }

    private String checkUniqTitle(String message_text, long chatID) {

        int k = 1;
        map.get(chatID).setRecipeTitles(dbController.recipeList(map.get(chatID).getUserID()));

        String regex = "(?iu)".concat(message_text).concat("[(]\\d{1,2}[)]$");

        for (String x : map.get(chatID).getRecipeTitles()) {
            if (x.equalsIgnoreCase(message_text)) {
                k++;
            }
            if (x.matches(regex)) {
                k = Integer.parseInt(x.replaceAll("[^0-9]", ""));
                k++;
            }
        }
        message_text = message_text.substring(0, 1).toUpperCase() + message_text.substring(1).toLowerCase();
        if (k == 1) return message_text;
        else {
            return message_text.concat("(").concat(String.valueOf(k).concat(")"));
        }
    }

    private void deleteMessage(int messageID, long chatID) {
        DeleteMessage deleteMessage = new DeleteMessage(String.valueOf(chatID), messageID);
        try {
            execute(deleteMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private boolean checkIfServiceCommand(String msg){
        for (String cmd : serviceCommands){
            if (cmd.equals(msg)) return true;
        }
        return false;
    }

    private String getTitleOfMessage(String messageText) {
        return messageText.split("\\R")[0];
    }

    private void createPurchaseList(long chatID) {
        CacheBot cacheBot = map.get(chatID);
        cacheBot.setPurchasedComponents(cacheBot.getPurcasedRecipe().getComponents());
        cacheBot.setClicked(new boolean[cacheBot.getPurchasedComponents().length]);
        for (boolean name : cacheBot.getClicked()) {
            name = false;
        }
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        String radio = "◽️  ";
        String tag = "PURCHASE;";

        for (int i = 0; i < cacheBot.getPurchasedComponents().length; i++) {
            linkedHashMap.put(radio.concat(cacheBot.getPurchasedComponents()[i]), tag.concat(String.valueOf(i)));
        }
        MySendMassage(myInlineKeyboard(linkedHashMap, 1), "Cписок покупок для рецепту:", chatID);
    }

    private void editPurchaseList(int k, int messageID, long chatID, String id) {
        CacheBot cacheBot = map.get(chatID);

        cacheBot.getClicked()[k] = !cacheBot.getClicked()[k];
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        String radio = "◽️  ";
        String tag = "PURCHASE;";
        for (int i = 0; i < cacheBot.getPurchasedComponents().length; i++) {
            if (cacheBot.getClicked()[i]) radio = "✅️  ";
            linkedHashMap.put(radio.concat(cacheBot.getPurchasedComponents()[i]), tag.concat(String.valueOf(i)));
            radio = "◽️  ";
        }
        EditMessageReplyMarkup editMessageReplyMarkup = new EditMessageReplyMarkup();
        editMessageReplyMarkup.setMessageId(messageID);
        editMessageReplyMarkup.setChatId(String.valueOf(chatID));
        editMessageReplyMarkup.setReplyMarkup(myInlineKeyboard(linkedHashMap, 1));
        try {
            execute(editMessageReplyMarkup);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void recipeCreateFromJSON(ArrayList<JSONObject> arrayList, long chatID) {
        CacheBot cacheBot = map.get(chatID);
        for (JSONObject jsonObject : arrayList) {
            cacheBot.setImportedRecipe(new Recipe());
            cacheBot.getImportedRecipe().setTitle(jsonObject.getString("title"));
            cacheBot.getImportedRecipe().setPreparing(jsonObject.getString("Cooking"));
            JSONArray jsonArray = jsonObject.getJSONArray("Components");
            String[] components = new String[jsonArray.length()];
            for (int i = 0; i < jsonArray.length(); i++) {
                components[i] = jsonArray.getString(i);
            }
            cacheBot.getImportedRecipe().setComponents(components);
            if (jsonObject.has("photo")) {
                String photo = jsonObject.getString("photo");
                byte[] decodedImg = Base64.getDecoder()
                        .decode(photo.getBytes());
                cacheBot.getImportedRecipe().setImage(decodedImg);
            }
            showRecipe(cacheBot.getImportedRecipe(),chatID);
        }
    }

    private void editInlineMenu(int messageID, long chatID) {
        EditMessageReplyMarkup editMessageReplyMarkup = new EditMessageReplyMarkup();
        editMessageReplyMarkup.setMessageId(messageID);
        editMessageReplyMarkup.setChatId(String.valueOf(chatID));
        editMessageReplyMarkup.setReplyMarkup(myInlineKeyboard(pageNumber,chatID));
        try {
            execute(editMessageReplyMarkup);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendServiceMessageToChat(String value, String id) {
        AnswerCallbackQuery answerCallbackQuery = new AnswerCallbackQuery();
        answerCallbackQuery.setCallbackQueryId(id);
        answerCallbackQuery.setText(value);
        answerCallbackQuery.setCacheTime(1200);
        answerCallbackQuery.setShowAlert(false);
        try {
            execute(answerCallbackQuery);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void editInlineMenu(int messageID, long chatID, InlineKeyboardMarkup inlineKeyboardMarkup) {
        EditMessageReplyMarkup editMessageReplyMarkup = new EditMessageReplyMarkup();
        editMessageReplyMarkup.setMessageId(messageID);
        editMessageReplyMarkup.setChatId(String.valueOf(chatID));
        editMessageReplyMarkup.setReplyMarkup(inlineKeyboardMarkup);
        try {
            execute(editMessageReplyMarkup);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void showRecipe(long chatID) {
        CacheBot cacheBot = map.get(chatID);
        MySendMassage("Ось ваш рецепт⬇️",chatID);
        if (cacheBot.getRecipe().getImage() != null) sendImageToChat(chatID);
        MySendMassage(myInlineKeyboard(new LinkedHashMap() {{
            put("\uD83D\uDCBE Зберегти в базу даних", "SAVE_TO_DB");
            put("✏️Редагувати", "EDIT_RECIPE");
            put("\uD83D\uDD19 Вийти", "EXIT_RECIPE");
        }}, 2), cacheBot.getRecipe().twoString(),chatID);
    }

    private void showRecipe(Recipe rec, long chatID) {
        CacheBot cacheBot = map.get(chatID);
        cacheBot.setPurcasedRecipe(rec);

        if (rec.getImage() != null) sendImageToChat(rec,chatID);
        MySendMassage(myInlineKeyboard(new LinkedHashMap() {{
            put("\uD83D\uDED2  Cформувати список покупок", "PURCHASE_LIST");
            put("✏️ Редагувати", "EDIT_IN_DB");
            put("〰️ Сховати меню", "CLOSE");
        }}, 1), rec.twoString(),chatID);
    }

    public void MySendMassage(ReplyKeyboardMarkup replyKeyboardMarkup, String text, long chatID) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableHtml(true);
        sendMessage.enableMarkdown(true);
        sendMessage.setReplyMarkup(replyKeyboardMarkup);
        sendMessage.setChatId(String.valueOf(chatID));
        sendMessage.setText(text);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void MySendMassage(InlineKeyboardMarkup inlineKeyboardMarkup, String text, long chatID) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setReplyMarkup(inlineKeyboardMarkup);
        sendMessage.setChatId(String.valueOf(chatID));
        sendMessage.setText(text);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void MySendMassage(String text, long chatID) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(String.valueOf(chatID));
        sendMessage.setText(text);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public ReplyKeyboardMarkup mainMenuKeyboard() {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setOneTimeKeyboard(true);
        replyKeyboardMarkup.setResizeKeyboard(true);

        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row1 = new KeyboardRow();
        KeyboardRow row2 = new KeyboardRow();
        row1.add(new KeyboardButton("Відкрити рецепт"));
        row1.add(new KeyboardButton("Додати новий рецепт"));
        row2.add(new KeyboardButton("Показати випадковий рецепт"));
        row2.add(new KeyboardButton("Інформація"));
        keyboard.add(row1);
        keyboard.add(row2);
        replyKeyboardMarkup.setKeyboard(keyboard);
        return replyKeyboardMarkup;
    }

    public InlineKeyboardMarkup myInlineKeyboard(int page, long chatID) {
        CacheBot cacheBot = map.get(chatID);
        cacheBot.getRecipeTitles().sort(String::compareToIgnoreCase);
        double pages = Math.ceil((double) cacheBot.getRecipeTitles().size() / (double) qty);
        if (page > pages) page = (int) pages;
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> rowInline = new ArrayList<>();
        String str = "OPEN_RECIPE";

        for (int i = qty * page - qty; i < qty * page; i++) {
            if (i < cacheBot.getRecipeTitles().size()) {
                rowInline.add(InlineKeyboardButton.builder()
                        .text(cacheBot.getRecipeTitles().get(i)).
                        callbackData(str.concat(";").concat(cacheBot.getRecipeTitles().get(i)))
                        .build());
                rowsInline.add(cloneList(rowInline));
                rowInline.clear();
            }
        }

        if (page == 1) {
            rowInline.add(InlineKeyboardButton.builder()
                    .text("➡").
                    callbackData("NEXT")
                    .build());
        } else rowInline.add(InlineKeyboardButton.builder()
                .text("⬅️").
                callbackData("BACK")
                .build());

        rowInline.add(InlineKeyboardButton.builder()
                .text(String.valueOf(page).concat("/").concat(String.valueOf((int) pages))).
                callbackData("a")
                .build());

        if (page == pages) {
            rowInline.add(InlineKeyboardButton.builder()
                    .text("⬅").
                    callbackData("BACK")
                    .build());
        } else rowInline.add(InlineKeyboardButton.builder()
                .text("➡️").
                callbackData("NEXT")
                .build());
        rowsInline.add(rowInline);
        markupInline.setKeyboard(rowsInline);
        return markupInline;
    }

    public InlineKeyboardMarkup myInlineKeyboard(LinkedHashMap linkedHashMap, int number) {
        int k = 0;

        Set set = linkedHashMap.entrySet();
        Iterator i = set.iterator();

        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> rowInline = new ArrayList<>();

        while (i.hasNext()) {
            if (k < number) {
                Map.Entry me = (Map.Entry) i.next();
                rowInline.add(InlineKeyboardButton.builder()
                        .text((String) me.getKey()).
                        callbackData((String) me.getValue())
                        .build());
                k++;
            }
            if (k == number || !i.hasNext()) {
                rowsInline.add(cloneList(rowInline));
                rowInline.clear();
                k = 0;
            }


        }

        markupInline.setKeyboard(rowsInline);
        return markupInline;
    }

    public static List<InlineKeyboardButton> cloneList(List<InlineKeyboardButton> list) {
        List<InlineKeyboardButton> clone = new ArrayList<InlineKeyboardButton>(list.size());
        for (InlineKeyboardButton item : list) clone.add(item);
        return clone;
    }

    public void sendImageToChat(long chatID) {
        CacheBot cacheBot = map.get(chatID);
        SendPhoto sendPhoto = new SendPhoto();
        // sendPhoto.setCaption("asdsad");
        sendPhoto.setChatId(String.valueOf(chatID));
        InputFile inputFile = new InputFile();
        inputFile.setMedia(new String(cacheBot.getRecipe().getImage()));
        sendPhoto.setPhoto(inputFile);
        try {
            execute(sendPhoto);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void sendImageToChat(Recipe rec, long chatID) {
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(String.valueOf(chatID));
        InputFile inputFile = new InputFile();
        inputFile.setMedia(new String(rec.getImage()));
        sendPhoto.setPhoto(inputFile);
        try {
            execute(sendPhoto);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getBotUsername() {
        return "Recipe4Bot";
    }

    @Override
    public String getBotToken() {
        return "5168644073:AAHj0roR_H7qexqHlJnOrdqPsyFuCWLHXMA";
    }




}
