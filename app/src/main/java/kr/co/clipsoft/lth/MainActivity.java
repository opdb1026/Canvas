package kr.co.clipsoft.lth;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.EmbossMaskFilter;
import android.graphics.MaskFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private static final int MY_PERMISSION_STORAGE = 1111;
    private Paint mPaint;
    private MaskFilter mEmboss;
    private MaskFilter mBlur;
    private Toolbar tb;
    private Boolean mEraserMode = false;
    private MyView view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        view = new MyView(this);
        setContentView(view);
        //접근 권한 확인
        checkPermission();
        //java 만들지 않고 다른 view 만들지 않고 바로 생성
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.activity_main,null);
        addContentView(v , new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,FrameLayout.LayoutParams.MATCH_PARENT));
        //툴바 설정
        tb = (Toolbar) findViewById(R.id.app_toolbar) ;
        setSupportActionBar(tb);
        //펜 클래스
        mPaint = new Paint();
        //표면 부드럽게
        mPaint.setAntiAlias(true);
        //장비에 맞춰 출력
        mPaint.setDither(true);
        //색상 지정
        mPaint.setColor(0xFF000000);
        //펜 스타일
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        //펜 굵기
        mPaint.setStrokeWidth(6);
        //빛과 그림자(효과)
        // f = float
        mEmboss = new EmbossMaskFilter(new float[] { 1, 1, 1 },
                0.4f, 6, 3.5f);
        //후광(번짐)
        mBlur = new BlurMaskFilter(8, BlurMaskFilter.Blur.NORMAL);
    }

    //메뉴 불러오기
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.appbar_action, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.first:
                view.eraseAll();
                return true;
            case R.id.pen:
                mPaint.setColor(0xFF000000);
                mEraserMode = false;
                return true;
            case R.id.eraser:
                mEraserMode = true;
                return true;
            case R.id.undo:
                view.onClickUndo();
                return true;
            case R.id.redo:
                view.onClickRedo();
                return true;
            default:
                //false
                return super.onOptionsItemSelected(item);
        }
    }

    private void checkPermission(){
        //접근권한 되어있는지 확인
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // 처음 호출시엔 if()안의 부분은 false로 리턴 됨
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                new AlertDialog.Builder(this)
                        .setTitle("알림")
                        .setMessage("저장소 권한이 거부되었습니다. 사용을 원하시면 설정에서 해당 권한을 직접 허용하셔야 합니다.")
                        //취소
                        .setNeutralButton("설정", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                intent.setData(Uri.parse("package:" + getPackageName()));
                                startActivity(intent);
                            }
                        })
                        //확인
                        .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                finish();
                            }
                        })
                        //화면 고정
                        .setCancelable(false)
                        .create()
                        .show();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSION_STORAGE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSION_STORAGE:
                for (int i = 0; i < grantResults.length; i++) {
                    // grantResults[] : 허용된 권한은 0, 거부한 권한은 -1
                    if (grantResults[i] < 0) {
                        Toast.makeText(MainActivity.this, "해당 권한을 활성화 하셔야 합니다.", Toast.LENGTH_SHORT).show();
                        new Handler().postDelayed(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                android.os.Process.killProcess(android.os.Process.myPid());
                            }
                        }, 2000);// 1초 정도 딜레이를 준 후 시작
                        return;
                    }
                }
                break;
        }
    }

    @Override
    // 뒤로가기 입력
    public void onBackPressed() {
        // 다이얼로그 생성
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("알림");
        builder.setMessage("앱을 종료하시겠습니까?");
        builder.setNegativeButton("취소", null);
        builder.setPositiveButton("종료", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // 종료
                android.os.Process.killProcess(android.os.Process.myPid());
            }
        });
        builder.show();
    }

    public class MyView extends View implements View.OnTouchListener {

        private Canvas mCanvas;
        //이미지 표현
        private Bitmap mBitmap;
        //펜 경로
        private Path mPath;
        //지우개용 사각형
        private Rect mRect;
        //스택 배열 이용
        private int c;
        private int delP;
        private int[] order;
        //그린 경로 포인트 추출
        private ArrayList<Path> redoPaths = new ArrayList<Path>();
        private ArrayList<Path> temPaths = new ArrayList<Path>();
        private PathMeasure mPathMeasure;
        private PointF pointPosition;
        private float mX, mY;
        private float totalLength;
        private float distancePerFrame;
        private float[] position = new float[ 2 ];
        private float[] tangent = new float[ 2 ];

        //터치시 그냥 터치인지 드래그인지
        private static final float TOUCH_TOLERANCE = 4;
        //경로 프레임 나눠 연산
        private  static  final int FRAMECOUNT = 60;

        //생성자
        public MyView(Context context) {
            super(context);
            this.setOnTouchListener(this);
            mCanvas = new Canvas();
            mPath = new Path();
            mRect = new Rect();
            c = 0;
            order = new int[100];
            //res >> bitmap
            mBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher_background);
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
        }

        //invalidate
        @Override
        protected void onDraw(Canvas canvas) {
            //canvas.drawPath(mPath, mPaint);
            for (Path p : redoPaths) {
                canvas.drawPath(p, mPaint);
            }
            if(mEraserMode){
                mRect.set((int)mX - 10, (int)mY - 10, (int)mX + 10, (int)mY + 10);
            }
            else{
                canvas.drawPath(mPath, mPaint);
            }
        }

        //초기화
        private void eraseAll(){
            redoPaths.clear();
            temPaths.clear();
            c = 0;
            Arrays.fill(order,0);
            mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            invalidate();
        }

        //지우개 메소드
        private void eraseCheck(){
            boolean finished = false;
            for (int i = 0; i < redoPaths.size(); i++) {
                mPathMeasure = new PathMeasure( redoPaths.get(i), false );
                totalLength = mPathMeasure.getLength();
                distancePerFrame = totalLength / FRAMECOUNT;
                for( int frame = 0; frame < FRAMECOUNT; frame++ ){
                    mPathMeasure.getPosTan( distancePerFrame * frame, position, tangent);
                    pointPosition = new PointF( position[ 0 ], position[ 1 ] );
                    if ((mRect.contains((int)pointPosition.x + 10,(int)pointPosition.y + 10) ||
                            (mRect.contains((int)pointPosition.x - 10,(int)pointPosition.y - 10)))){
                        finished = true;
                        break;
                    }
                }
                if (finished == true){
                    temPaths.add(redoPaths.remove(i));
                    order[c++] = 2;
                    delP = i;
                    return;
                }
            }
            return;
        }

        private void onClickUndo() {
            Log.v("size",Integer.toString(temPaths.size()));
            Log.v("c",Integer.toString(c));
            if (c - 1 > -1) {
                if (order[c - 1] == 1) {
                    if (delP != 0){

                    }
                    c--;
                    redoPaths.remove(redoPaths.size() - 1);
                } else if (order[c - 1] == 2) {
                    c--;
                    redoPaths.add(temPaths.get(c));
                }
                invalidate();
            }
        }

        private void onClickRedo() {
            Log.v("size",Integer.toString(temPaths.size()));
            Log.v("c",Integer.toString(c));
            if (c < temPaths.size()) {
                if (order[c] == 1) {
                    redoPaths.add(temPaths.get(c++));
                } else if (order[c] == 2) {
                    redoPaths.remove(redoPaths.size() - 1);
                }
                invalidate();
            }
        }

        private void touch_start(float x, float y) {
            mPath.reset();
            mPath.moveTo(x, y);
            mX = x;
            mY = y;
        }

        private void touch_move(float x, float y) {
            float dx = Math.abs(x - mX);
            float dy = Math.abs(y - mY);
            if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
                mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
                mX = x;
                mY = y;
                if(mEraserMode){
                    eraseCheck();
                }
                else{
                    mCanvas.drawPath(mPath, mPaint);
                }
            }
        }

        private void touch_up() {
            mPath.lineTo(mX, mY);
            // commit the path to our offscreen
            if(mEraserMode){
                eraseCheck();
            }
            else{
                if (order[c] != 0){
                    int i = c;
                    while (i < temPaths.size()){
                        order[i] = 0;
                        temPaths.remove(i);
                        i++;
                    }
                }
                mCanvas.drawPath(mPath, mPaint);
                redoPaths.add(mPath);
                temPaths.add(mPath);
                order[c++] = 1;
            }
            // kill this so we don't double draw
            mPath = new Path();
        }

        @Override
        public boolean onTouch(View arg0, MotionEvent event) {
            float x = event.getX();
            float y = event.getY();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    touch_start(x, y);
                    break;
                case MotionEvent.ACTION_MOVE:
                    touch_move(x, y);
                    break;
                case MotionEvent.ACTION_UP:
                    touch_up();
                    break;
            }
            invalidate();
            return true;
        }
    }
}
