package com.obar;

import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) {
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

        Label title = new Label("OBAR Desktop Teste");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold;");

        StackPane root = new StackPane(title);
        root.setAlignment(Pos.CENTER);

        Scene scene = new Scene(root, 700, 420);
        stage.setTitle("OBAR TESTE");
        stage.setScene(scene);
        stage.show();
    }

}
