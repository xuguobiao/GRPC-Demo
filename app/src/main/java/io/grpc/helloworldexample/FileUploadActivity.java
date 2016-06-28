package io.grpc.helloworldexample;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.kido.fileuploader.nano.FileReply;
import io.grpc.kido.fileuploader.nano.FileRequest;
import io.grpc.kido.fileuploader.nano.UploaderGrpc;
import io.grpc.stub.StreamObserver;

/**
 * 该类只是简单的演示，未必能work
 */
public class FileUploadActivity extends Activity implements View.OnClickListener {

    private String mHost = "";
    private int mPort = 200;
    private ManagedChannel mChannel;
    private UploaderGrpc.UploaderBlockingStub blockingStub;
    private UploaderGrpc.UploaderStub asyncStub;


    private Button choosePicButton, uploadButton;
    private TextView messageTextView;

    private File imageFile;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_upload);

        choosePicButton = (Button) findViewById(R.id.choose_pic_btn);
        uploadButton = (Button) findViewById(R.id.upload_btn);
        messageTextView = (TextView) findViewById(R.id.message_tv);

        choosePicButton.setOnClickListener(this);
        uploadButton.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.choose_pic_btn:
                openPicChooser();
                break;
            case R.id.upload_btn:

                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICTURE && resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};

            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();

            // String picturePath contains the path of selected Image
            imageFile = new File(picturePath);
            messageTextView.setText(imageFile.getAbsolutePath());
        }
    }

    private static final int PICTURE = 10086; //requestcode

    private void openPicChooser() {
        Intent intent = new Intent();
        if (Build.VERSION.SDK_INT < 19) {//因为Android SDK在4.4版本后图片action变化了 所以在这里先判断一下
            intent.setAction(Intent.ACTION_GET_CONTENT);
        } else {
            intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
        }
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, PICTURE);
    }


    private class GrpcTask extends AsyncTask<Void, Void, Void> {


        @Override
        protected void onPreExecute() {
        }


        @Override
        protected Void doInBackground(Void... nothing) {
            try {
                mChannel = ManagedChannelBuilder.forAddress(mHost, mPort)
                        .usePlaintext(true)
                        .build();
                blockingStub = UploaderGrpc.newBlockingStub(mChannel);
                asyncStub = UploaderGrpc.newStub(mChannel);

                File file = new File("");
                uploadFile(file);

            } catch (Exception e) {
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            try {
                mChannel.shutdown().awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }


    private void uploadFile(File file) throws Exception {


        StreamObserver<FileReply> responseObserver = new StreamObserver<FileReply>() {
            @Override
            public void onNext(FileReply reply) {

            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onCompleted() {

            }
        };

        StreamObserver<FileRequest> requestObserver = asyncStub.uploadFile(responseObserver);

        try {
            FileRequest fileRequest = new FileRequest();
            BufferedInputStream bInputStream = new BufferedInputStream(new FileInputStream(file));
            int bufferSize = 512 * 1024; // 512k
            byte[] buffer = new byte[bufferSize];

            int tmp = 0;
            int size = 0;
            if ((tmp = bInputStream.read(buffer)) > 0) {
                size += tmp;
                fileRequest.data = buffer;
                fileRequest.offset = size;

                requestObserver.onNext(fileRequest);
            }

        } catch (Exception e) {
            // Cancel RPC
            requestObserver.onError(e);
            throw e;
        }
        // Mark the end of requests
        requestObserver.onCompleted();

    }

}
