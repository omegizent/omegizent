/*

Copyright 2025-2026 Jeffrey J. Weston <jjweston@gmail.com>

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

*/

package org.omegizent.gui;

import org.omegizent.embedding.EmbeddingCacheService;
import org.omegizent.embedding.EmbeddingService;
import org.omegizent.embedding.SQLiteConnectionFactory;
import org.omegizent.ingest.markdown.MarkdownLoader;
import org.omegizent.openai.EmbeddingApiService;
import org.omegizent.openai.OpenAiApiCaller;
import org.omegizent.openai.ResponseApiService;
import org.omegizent.qdrant.QdrantService;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.nio.file.Paths;
import java.sql.Connection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class QueryGui extends Application
{
    private VBox conversationBox;
    private ScrollPane scrollPane;
    private TextArea inputArea;
    private Button sendButton;

    private Connection connection;
    private QdrantService qdrantService;
    private ResponseApiService responseApiService;

    public void init()
    {
        SQLiteConnectionFactory sqLiteConnectionFactory = new SQLiteConnectionFactory();
        this.connection = sqLiteConnectionFactory.create();
        this.qdrantService = new QdrantService();
        OpenAiApiCaller openAiApiCaller = new OpenAiApiCaller();
        EmbeddingCacheService embeddingCacheService = new EmbeddingCacheService( this.connection );
        EmbeddingApiService embeddingApiService = new EmbeddingApiService( openAiApiCaller );
        EmbeddingService embeddingService = new EmbeddingService( embeddingCacheService, embeddingApiService );
        this.responseApiService = new ResponseApiService(
                embeddingCacheService, embeddingService, this.qdrantService, openAiApiCaller );

        MarkdownLoader markdownLoader = new MarkdownLoader( embeddingService, this.qdrantService );
        markdownLoader.load( Paths.get( "readme.md" ));
    }

    public void stop()
    {
        List< RuntimeException > exceptions = new LinkedList<>();

        try { this.connection.close(); }
        catch ( Exception e )
        {
            exceptions.add( new RuntimeException( "Exception occurred while closing database connection.", e ));
        }

        try { this.qdrantService.close(); }
        catch ( Exception e )
        {
            exceptions.add( new RuntimeException( "Exception occurred while closing Qdrant service.", e ));
        }

        if ( !exceptions.isEmpty() )
        {
            if ( exceptions.size() == 1 ) throw exceptions.getFirst();

            RuntimeException exception = new RuntimeException( "Exceptions occurred while stopping." );
            for ( Exception e : exceptions ) exception.addSuppressed( e );
            throw exception;
        }
    }

    public void start( Stage stage )
    {
        this.conversationBox = new VBox();

        this.scrollPane = new ScrollPane( this.conversationBox );
        this.scrollPane.setFitToWidth( true );

        this.inputArea = new TextArea();
        this.inputArea.setWrapText( true );

        this.sendButton = new Button( "Send" );

        HBox inputRow = new HBox( this.inputArea, this.sendButton );
        HBox.setHgrow( this.inputArea, Priority.ALWAYS );

        BorderPane root = new BorderPane();
        root.setCenter( this.scrollPane );
        root.setBottom( inputRow );

        this.conversationBox.heightProperty().addListener( _ -> this.scrollPane.setVvalue( 1.0 ));
        this.sendButton.setOnAction( _ -> this.sendUserMessage() );

        this.addApiMessage( "Welcome to Omegizent. How can I help?" );

        Scene scene = new Scene( root, 800, 600 );

        stage.setTitle( "Omegizent" );
        stage.setScene( scene );
        stage.show();
    }

    private void addUsrMessage( String text )
    {
        Label label = new Label( text );
        label.setWrapText( true );
        label.setBackground( Background.fill( Color.LIGHTGRAY ));
        this.conversationBox.getChildren().add( label );
    }

    private void addApiMessage( String text )
    {
        Label label = new Label( text );
        label.setWrapText( true );
        this.conversationBox.getChildren().add( label );
    }

    private void sendUserMessage()
    {
        String text = this.inputArea.getText().trim();
        this.inputArea.clear();
        if ( text.isEmpty() ) return;

        this.inputArea.setDisable( true );
        this.sendButton.setDisable( true );
        this.addUsrMessage( text );

        CompletableFuture
                .supplyAsync( () -> this.responseApiService.getResponse( text ))
                .thenAccept( response -> Platform.runLater( () -> this.processApiResponse( response )))
                .exceptionally( e -> { System.out.println( "Exception Occurred: " + e ); return null; } );
    }

    private void processApiResponse( String response )
    {
        this.inputArea.setDisable( false );
        this.sendButton.setDisable( false );
        this.addApiMessage( response );
        this.inputArea.requestFocus();
    }
}
