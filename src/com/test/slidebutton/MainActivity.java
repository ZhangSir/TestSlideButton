package com.test.slidebutton;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.widget.Toast;

import com.test.slidebutton.SlideButton.SlideListener;

public class MainActivity extends ActionBarActivity {

	private SlideButton sb1, sb2, sb3;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		sb1 = (SlideButton) this.findViewById(R.id.sb1);
		sb2 = (SlideButton) this.findViewById(R.id.sb2);
		sb3 = (SlideButton) this.findViewById(R.id.sb3);
		
		sb3.setSlideListener(new SlideListener() {
			
			@Override
			public void open() {
				// TODO Auto-generated method stub
				sb1.setOpen(true);
				sb2.setOpen(true);
			}
			
			@Override
			public void close() {
				// TODO Auto-generated method stub
				sb1.setOpen(false);
				sb2.setOpen(false);
			}
		});
		
		sb2.setSlideListener(new SlideListener() {
			
			@Override
			public void open() {
				// TODO Auto-generated method stub
				Toast.makeText(MainActivity.this, "sb2´ò¿ª", Toast.LENGTH_SHORT).show();
			}
			
			@Override
			public void close() {
				// TODO Auto-generated method stub
				Toast.makeText(MainActivity.this, "sb2¹Ø±Õ", Toast.LENGTH_SHORT).show();
			}
		});
	}

}
