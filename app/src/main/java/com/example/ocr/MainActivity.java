package com.example.ocr;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;


import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.IOException;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int PICK_IMAGE = 123;// Request code for image selection activity result
    //widgets
  ImageView imageView;
  TextView textView;
  Button imageBtn, speechBtn;

  //variables
    InputImage inputImage; // InputImage object for ML Kit's text recognition
    TextRecognizer recognizer; // TextRecognizer object for recognizing text
    TextToSpeech textToSpeech;// TextToSpeech engine for reading the recognized text
    public Bitmap textImage;  // Bitmap to hold the selected image

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Initialize the TextRecognizer with default options
        recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        imageView = findViewById(R.id.img_view);
        textView = findViewById(R.id.text);
        imageBtn = findViewById(R.id.choose_image);
        speechBtn = findViewById(R.id.speech);

        imageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //open gallery to allow you to select an image from gallery
                openGallery();

            }
        });


        // Initialize the TextToSpeech engine and set language to US English
        textToSpeech=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                // assign language you can scan  to US if initialization is successful by default you can change it
                if(status != TextToSpeech.ERROR) {
                    textToSpeech.setLanguage(Locale.US);
                }
            }
        });


        // Set an OnClickListener for the speech button to convert text to speech
        speechBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Speak out the text displayed in the TextView
                textToSpeech.speak(textView.getText().toString(),TextToSpeech.QUEUE_FLUSH,null);
            }
        });


    }
    // Method to open the gallery and allow the user to pick an image
    private void openGallery() {
        // Create an Intent to pick an image from the device's gallery
        Intent getIntent = new Intent(Intent.ACTION_GET_CONTENT);
        getIntent.setType("image/");// Set type to image


        // Create another Intent to access images stored in external storage (gallery)
        Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickIntent.setType("image/");

        // Create a chooser to let the user select an image using their preferred app
        Intent chooserIntent = Intent.createChooser(getIntent, "Select Image");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] {pickIntent});
// Start the activity for result with the specified request code
        startActivityForResult(chooserIntent, PICK_IMAGE);

    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Check if the result is for the PICK_IMAGE request and data is not null
        if (requestCode == PICK_IMAGE) {
            if (data != null) {
               byte[] byteArray=new byte[0];
               String filePath=null;

                // convert uri to bitmap
                try {
                    // Convert the selected image's URI to an InputImage object

                    inputImage = InputImage.fromFilePath(this, data.getData());


                    // Get the bitmap representation of the selected image
                    Bitmap resultUri = inputImage.getBitmapInternal();

                    // Use Glide library to load and display the selected image in the ImageView
                    Glide.with(MainActivity.this).load(resultUri).into(imageView);

                    // after loading the image we need to start reading processed text block

                    Task<Text> result =recognizer.process(inputImage)
                            // Define success behavior when text recognition succeeds
                            .addOnSuccessListener(new OnSuccessListener<Text>() {
                                @Override
                                // Process the recognized text and display it in the TextView

                                public void onSuccess(Text text) {
                                    ProcessTextBlock(text);
                                }

                            })
                            // Define failure behavior when text recognition fails

                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                   Toast.makeText(MainActivity.this,e.getMessage(),Toast.LENGTH_SHORT).show();
                                }
                            });



                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    // Method to process recognized text blocks and display them in the TextView
    private void ProcessTextBlock(Text text) {
        // start ML kit=> process the text block
        // Get the full recognized text from the image
        String resultText = text.getText();

        // Iterate through each text block recognized in the image
        for (Text.TextBlock block:text.getTextBlocks()){
            String blockText = block.getText(); // Get text from each block
            textView.append("\n"); // new line after block of text

            //graphics
            // Get corner points and bounding box for graphical representation (optional)
            Point[] blockCornerPoints = block.getCornerPoints();
            Rect blockFrame = block.getBoundingBox();// Get bounding box for block

            // Iterate through each line of text within the block
            for (Text.Line line:block.getLines()){
                String lineText = line.getText();  // Get text from each line

                Point[] lineCornerPoints = line.getCornerPoints();
                Rect lineFrame = line.getBoundingBox(); //bounding box of line

                // Iterate through each element (word) in the line
                for (Text.Element element:line.getElements()){
                  textView.append(" ");
                  String elementText = element.getText(); // Get text from each element
                  textView.append(elementText);// Append text to the TextView

                  Point[] elementCornerPoints = element.getCornerPoints();
                  Rect elementFrame = element.getBoundingBox(); //bounding box of element
                }
            }
        }

    }

    @Override
    // Check if the TextToSpeech engine is not speaking before calling super

    protected void onPause() {
        if (!textToSpeech.isSpeaking()) {
            super.onPause();
        }


    }
}