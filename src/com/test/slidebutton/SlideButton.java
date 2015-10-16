/*
 * Copyright (C) 2015 Quinn Chen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.test.slidebutton;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Looper;
import android.os.Parcelable;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

/**
 * 滑动开关按钮
 * @author zhangshuo
 */
public class SlideButton extends View {

	public static final int SHAPE_RECT = 1;
	public static final int SHAPE_RECT_CIRCLE = 2;
	public static final int SHAPE_CIRCLE = 3;
	
	private static final int DEFAULT_COLOR_OPENED = Color.parseColor("#ff00ee00");
	private static final int DEFAULT_COLOR_CLOSED = Color.parseColor("#ffeeeeee");
	private static final int DEFAULT_COLOR_BALL = Color.parseColor("#ffffffff");
	private static final int RIM_SIZE = 6;
	
	/**组件默认宽度*/
	private int DEFAULT_WIDTH = 64;
	/**组件默认高度*/
	private int DEFAULT_HEIGHT = 32;
	
	private Context mContext;
	
	private int colorOpend = DEFAULT_COLOR_OPENED;
	private int colorClosed = DEFAULT_COLOR_CLOSED;
	private int colorSlider = DEFAULT_COLOR_BALL;
	private boolean slidable = true;
	private boolean isOpen = false;
	private int shape = SHAPE_RECT;
	
	private Paint paint;
	private RectF rectfSlider;
	private RectF rectfBack;
	private int alpha;
	private int max_left;
	private int min_left;
	/**滑块当前的left位置*/
	private int sliderCurrentLeft;
	/**滑块直径或长宽(滑块的长宽相等)*/
	private int silderWidth;
	/**滑块的圆角半径*/
	private int sliderArcRadius;
	/**背景的圆角半径*/
	private float backArcRadius;
	/**滑块滑动时left的起始位置X*/
	private int sliderMoveStartLeft = RIM_SIZE;
	
	private int startX;
	private int startY;
	private int lastX;
	private int lastY;
	private int offsetX = 0;
	
	/**判断用户手指的移动距离是否足以响应为move*/
	private int touchSlop;
	
	/**
	 * 标示是否开始滑动
	 */
	private boolean canMove = false;
	
	private SlideListener listener;

	/**
	 * 滑动按钮监听器
	 * @author zhangshuo
	 */
	public interface SlideListener {
		public void open();

		public void close();
	}

	public SlideButton(Context context) {
		this(context, null);
	}
	
	public SlideButton(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}
	
	public SlideButton(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		this.mContext = context;
		this.touchSlop = ViewConfiguration.get(this.mContext).getScaledTouchSlop();
		this.DEFAULT_WIDTH = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 
				this.DEFAULT_WIDTH, getResources().getDisplayMetrics());
		this.DEFAULT_HEIGHT = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 
				this.DEFAULT_HEIGHT, getResources().getDisplayMetrics());
		listener = null;
		paint = new Paint();
		paint.setAntiAlias(true);
		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.slide_button);
		initStyle(a);
		a.recycle();
	}
	/**
	 * 初始化主题风格
	 * @param typedArray
	 */
	private void initStyle(TypedArray typedArray){
		if(typedArray.getIndexCount() > 0){
			int typedCount = typedArray.getIndexCount();
			int typed = -1;
			for (int i=0; i<typedCount; i++) {
				typed = typedArray.getIndex(i);
				if (R.styleable.slide_button_slideButtonStyle == typed) {
					int resId = typedArray.getResourceId(typed, 0);
					TypedArray a1 = mContext.obtainStyledAttributes(resId, R.styleable.slide_button);
					initStyle(a1);
					a1.recycle();
				}else if(R.styleable.slide_button_colorOpened == typed){
					colorOpend = typedArray.getColor(typed, DEFAULT_COLOR_OPENED);
				}else if(R.styleable.slide_button_colorClosed == typed){
					colorClosed = typedArray.getColor(typed, DEFAULT_COLOR_CLOSED);
				}else if(R.styleable.slide_button_colorBall == typed){
					colorSlider = typedArray.getColor(typed, DEFAULT_COLOR_BALL);
				}else if(R.styleable.slide_button_slidable == typed){
					slidable = typedArray.getBoolean(typed, true);
				}else if(R.styleable.slide_button_isOpen == typed){
					isOpen = typedArray.getBoolean(typed, false);
				}else if(R.styleable.slide_button_shape == typed){
					shape = typedArray.getInt(typed, SHAPE_RECT);
				}
			}
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		int width = measureDimension(DEFAULT_WIDTH, widthMeasureSpec);
		int height = measureDimension(DEFAULT_HEIGHT, heightMeasureSpec);
		if (width < height){
			width = height * 2;
		}
		setMeasuredDimension(width, height);
		initDrawingVal();
	}
	
	public int measureDimension(int defaultSize, int measureSpec) {
		int result;
		int specMode = MeasureSpec.getMode(measureSpec);
		int specSize = MeasureSpec.getSize(measureSpec);
		if (specMode == MeasureSpec.EXACTLY) {
			result = specSize;
		} else {
			result = defaultSize; // UNSPECIFIED
			if (specMode == MeasureSpec.AT_MOST) {
				result = Math.min(result, specSize);
			}
		}
		return result;
	}

	public void initDrawingVal() {
		int width = getMeasuredWidth();
		int height = getMeasuredHeight();

		rectfBack = new RectF(0, 0, width, height);
		rectfSlider = new RectF();
		
		silderWidth = height - 2 * RIM_SIZE;
		
		if(shape == SHAPE_RECT_CIRCLE){
			backArcRadius = height/5;
			sliderArcRadius = silderWidth/5;
		}else if(shape == SHAPE_CIRCLE){
			backArcRadius = height/2;
			sliderArcRadius = silderWidth/2;
		}else{
			backArcRadius = 0;
			sliderArcRadius = 0;
		}
		
		min_left = RIM_SIZE;
		
		max_left = width - RIM_SIZE - silderWidth;
		
		if (isOpen) {
			sliderCurrentLeft = max_left;
			alpha = 255;
		} else {
			sliderCurrentLeft = RIM_SIZE;
			alpha = 0;
		}
		sliderMoveStartLeft = sliderCurrentLeft;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		paint.setColor(colorClosed);
		canvas.drawRoundRect(rectfBack, backArcRadius, backArcRadius, paint);
			
		paint.setColor(colorOpend);
		paint.setAlpha(alpha);
		canvas.drawRoundRect(rectfBack, backArcRadius, backArcRadius, paint);
			
		rectfSlider.set(sliderCurrentLeft, RIM_SIZE, sliderCurrentLeft + silderWidth, RIM_SIZE + silderWidth);
		paint.setColor(colorSlider);
		canvas.drawRoundRect(rectfSlider, sliderArcRadius, sliderArcRadius, paint);
	}
	
	
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (slidable == false) return super.onTouchEvent(event);
		
		int action = MotionEventCompat.getActionMasked(event);
		switch (action) {
		case MotionEvent.ACTION_DOWN:
			startX = (int) event.getRawX();
			startY = (int) event.getRawY();
			break;
		case MotionEvent.ACTION_MOVE:
			lastX = (int) event.getRawX();
			lastY = (int) event.getRawY();
			
			offsetX = lastX - startX;
			
			if(!this.canMove){
				if(Math.abs(offsetX) > touchSlop && Math.abs(offsetX) > Math.abs(lastY - startY)){
					this.canMove = true;
					return true;
				}else{
					return super.onTouchEvent(event);
				}
			}else{
				sliderCurrentLeft = offsetX + sliderMoveStartLeft;
				if(sliderCurrentLeft > max_left){
					sliderCurrentLeft = max_left;
				}else if(sliderCurrentLeft < min_left){
					sliderCurrentLeft = min_left;
				}
				alpha = (int)(255 * ((sliderCurrentLeft - min_left) / (float)(max_left - min_left)));
				invalidateView();
			}
			break;
		case MotionEvent.ACTION_UP:
			lastX = (int) event.getRawX();
			boolean toRight = (sliderCurrentLeft > max_left / 2 ? true : false);
			if(this.canMove){
				this.canMove = false;
			}else{//没有滑动，相应为点击操作，滑块移动到反方向
				toRight = !toRight;
			}
			moveToDest(toRight);
			break;
		default:
			break;
		}
		return true;
	}

	/**
	 * 通知View进行重绘
	 */
	private void invalidateView() {
		if (Looper.getMainLooper() == Looper.myLooper()) {
			invalidate();
		} else {
			postInvalidate();
		}
	}

	/**
	 * 设置按钮监听器
	 * @param listener
	 */
	public void setSlideListener(SlideListener listener) {
		this.listener = listener;
	}

	public void moveToDest(final boolean toRight) {
		ValueAnimator toDestAnim = ValueAnimator.ofInt(sliderCurrentLeft,
				toRight ? max_left : min_left);
		toDestAnim.setDuration(200);
		toDestAnim.setInterpolator(new AccelerateDecelerateInterpolator());
//		toDestAnim.setInterpolator(new OvershootInterpolator());
		toDestAnim.start();
		toDestAnim.addUpdateListener(new AnimatorUpdateListener() {

			@Override
			public void onAnimationUpdate(ValueAnimator animation) {
				sliderCurrentLeft = (Integer) animation.getAnimatedValue();
				alpha = (int) (255 * (float) sliderCurrentLeft / (float) max_left);
				if(alpha < 0) alpha = 0;
				if(alpha > 255) alpha = 255;
				invalidateView();
			}
		});
		toDestAnim.addListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationEnd(Animator animation) {
				if (toRight) {
					isOpen = true;
					if (listener != null){
						listener.open();
					}
					sliderCurrentLeft = sliderMoveStartLeft = max_left;
					alpha = 255;
				} else {
					isOpen = false;
					if (listener != null){
						listener.close();
					}
					sliderCurrentLeft = sliderMoveStartLeft = min_left;
					alpha = 0;
				}
				invalidateView();
			}
		});
	}

	/**
	 * 返回按钮状态
	 * @return
	 */
	public boolean isOpen() {
		return isOpen;
	}

	/**
	 * 设置按钮状态
	 * @param isOpen
	 */
	public void setOpen(boolean isOpen) {
		this.isOpen = isOpen;
		if(this.isOpen){
			moveToDest(true);
		}else{
			moveToDest(false);
		}
	}

	/**
	 * 返回按钮形状
	 * @return
	 */
	public int getShape() {
		return shape;
	}

	/**
	 * 设置按钮形状
	 * @param shape
	 */
	public void setShape(int shape) {
		this.shape = shape;
	}

	/**
	 * 返回按钮是否支持滑动操作
	 * @return
	 */
	public boolean isSlidable() {
		return slidable;
	}

	/**
	 * 设置按钮是否支持滑动操作
	 * @param slidable
	 */
	public void setSlidable(boolean slidable) {
		this.slidable = slidable;
	}

	@Override
	protected void onRestoreInstanceState(Parcelable state) {
		if (state instanceof Bundle) {
			Bundle bundle = (Bundle) state;
			this.isOpen = bundle.getBoolean("isOpen");
			state = bundle.getParcelable("instanceState");
		}
		super.onRestoreInstanceState(state);
	}

	@Override
	protected Parcelable onSaveInstanceState() {
		Bundle bundle = new Bundle();
		bundle.putParcelable("instanceState", super.onSaveInstanceState());
		bundle.putBoolean("isOpen", this.isOpen);
		return bundle;
	}
}
