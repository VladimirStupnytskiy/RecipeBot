import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;

public class Recipe {
    private byte[] image;
    private String title;
    private String [] components;


    public String[] getComponents() {
        return components;
    }

    public void setComponents(String[] components) {
        if (components != null) {
            for (int i = 0; i < components.length; i++) {
                if (components[i].matches("^\\d{1,2}[)].*")) {
                    components[i] = components[i].split("^\\d{1,2}\\)")[1];
                    components[i] = components[i].replaceAll("^\\s+","");
                }
            }
            this.components = components;
        }

    }

    public String twoString(){
        StringBuilder sb = new StringBuilder();
        sb.append("*").append(title).append("*\n\n");
        sb.append("*").append("Компоненти:").append("* \n");
        int i =1;
        if (components!=null) {
            for (String item : components) {
                sb.append(i).append(") ").append(item).append("\n");
                i++;
            }
        }
        sb.append("*").append("\nПриготування:").append("* \n");
        sb.append(preparing);

        return sb.toString();
    }

    public byte[] getImage() {
        return image;
    }

    public void setImage(byte[] image) {
        this.image = image;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }


    public String getPreparing() {
        return preparing;
    }

    public void setPreparing(String preparing) {
        this.preparing = preparing;
    }

    private String preparing;
}
