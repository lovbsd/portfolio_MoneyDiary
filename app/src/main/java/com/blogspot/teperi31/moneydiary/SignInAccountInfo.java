package com.blogspot.teperi31.moneydiary;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import java.util.HashMap;
import java.util.Map;

public class SignInAccountInfo extends AppCompatActivity implements View.OnClickListener {
	
	private FirebaseAuth mAuth;
	private EditText mIDField;
	private EditText mPasswordField;
	private TextView mIDText;
	private ProgressBar mProgressView;
	private ScrollView mLoginView;
	private ScrollView mLogoutView;
	private DatabaseReference mDatabase;
	
	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_signin);
		
		android.support.v7.widget.Toolbar mToolbar = findViewById(R.id.signin_toobarTop);
		mToolbar.setTitle("회원 정보");
		setSupportActionBar(mToolbar);
		
		// 네비게이션바
		findViewById(R.id.activity_signin_bottomBar_dashboardicon).setOnClickListener(this);
		findViewById(R.id.activity_signin_bottomBar_messengericon).setOnClickListener(this);
		findViewById(R.id.activity_signin_bottomBar_listicon).setOnClickListener(this);
		((ImageButton) findViewById(R.id.activity_signin_bottomBar_myinfoicon)).setImageResource(R.drawable.ic_action_myinfo_clicked);
		
		//
		mIDField = findViewById(R.id.signin_inputid);
		mPasswordField = findViewById(R.id.signin_inputpassword);
		mIDText = findViewById(R.id.signin_idresult);
		mProgressView = findViewById(R.id.signin_progress);
		mLoginView = findViewById(R.id.signin_loginscrollview);
		mLogoutView = findViewById(R.id.signin_logoutscrollview);
		mDatabase = FirebaseDatabase.getInstance().getReference();
		
		mAuth = FirebaseAuth.getInstance();
		
		findViewById(R.id.signin_loginButton).setOnClickListener(this);
		findViewById(R.id.signin_logoutButton).setOnClickListener(this);
		findViewById(R.id.signin_signupButton).setOnClickListener(this);
		
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		// 네비게이션 바 보이기
		findViewById(R.id.activity_signin_bottomBar).setVisibility(View.VISIBLE);
		mProgressView.setVisibility(View.VISIBLE);
		FirebaseUser currentUser = mAuth.getCurrentUser();
		if (currentUser != null) {
			updateUI(currentUser);
		} else {
			mProgressView.setVisibility(View.GONE);
		}
	}
	
	// 현재 로그인 토큰이 있는 경우 바로 다음 Activity로 넘어가기
	private void onAuthSuccess() {
		// 로그인 할 때 데이터 베이스에 내 정보 쌓기
		final FirebaseUser user = mAuth.getCurrentUser();
		
		mDatabase.child("users").orderByKey().equalTo(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
			@Override
			public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
				if (!dataSnapshot.hasChildren()) {
					DataUser datauser;
					// 유저 정보를 모아서
					if (user.getPhotoUrl() == null) {
						datauser = new DataUser(user.getUid(), user.getDisplayName(), user.getEmail(), null);
					} else {
						datauser = new DataUser(user.getUid(), user.getDisplayName(), user.getEmail(), user.getPhotoUrl().toString());
					}
					
					Map<String, Object> inputUserData = datauser.toMap();
					Map<String, Object> childUpdates = new HashMap<>();
					// Map 에 한번에 저장 후
					childUpdates.put("/users/" + user.getUid(), inputUserData);
					// 데이터베이스에 집어넣기
					mDatabase.updateChildren(childUpdates);
					
				}
				
				mProgressView.setVisibility(View.GONE);
				// Go to MainActivity : 메인으로 이동
				Toast.makeText(SignInAccountInfo.this, "환영합니다.\n" + user.getDisplayName(), Toast.LENGTH_SHORT).show();
				Intent i = new Intent(SignInAccountInfo.this, MainTestActivity.class);
				startActivity(i);
				// 이 페이지는 종료시킴
				finish();
				//페이드 아웃 애니메이션
				overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
			}
			
			@Override
			public void onCancelled(@NonNull DatabaseError databaseError) {
			
			}
		});
		
		
	}
	
	
	private void signIn(String ID, String password) {
		if (!validateForm()) {
			return;
		}
		mProgressView.setVisibility(View.VISIBLE);
		
		mAuth.signInWithEmailAndPassword(ID, password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
			@Override
			public void onComplete(@NonNull Task<AuthResult> task) {
				if (task.isSuccessful()) {
					onAuthSuccess();
				} else {
					Toast.makeText(SignInAccountInfo.this, "로그인에 실패했습니다.", Toast.LENGTH_SHORT).show();
					updateUI(null);
				}
				mProgressView.setVisibility(View.GONE);
				
			}
		});
		
	}
	
	private void signOut() {
		// 로그아웃 버튼을 누르는 순간 현재 접속자 목록에서 삭제
		mDatabase.child("users").orderByKey().equalTo(mAuth.getCurrentUser().getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
			@Override
			public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
				mAuth.signOut();
				updateUI(null);
			}
			
			@Override
			public void onCancelled(@NonNull DatabaseError databaseError) {
			
			}
		});
		
	}
	
	private boolean validateForm() {
		boolean valid = true;
		
		String email = mIDField.getText().toString();
		// ID 전용
		String ID = mIDField.getText().toString();
		if (TextUtils.isEmpty(ID)) {
			mIDField.setError("아이디를 입력하세요.");
			valid = false;
		} else if (!Patterns.EMAIL_ADDRESS.matcher(ID).matches()) {
			mIDField.setError("이메일 주소를 입력하세요.");
			valid = false;
		} else {
			mIDField.setError(null);
		}
		//Password error 전용
		String password = mPasswordField.getText().toString();
		
		if (TextUtils.isEmpty(password)) {
			mPasswordField.setError("비밀번호를 입력하세요.");
			valid = false;
		} else {
			mPasswordField.setError(null);
		}
		return valid;
	}
	
	
	private void updateUI(FirebaseUser user) {
		mProgressView.setVisibility(View.GONE);
		// 로그인 되어있는 경우
		if (user != null) {
			// Logout 버튼이 있는 창 띄우기
			mLoginView.setVisibility(View.GONE);
			mLogoutView.setVisibility(View.VISIBLE);
			// ID 를 보일 수 있는 부분에 ID 넣기
			mIDText.setText(user.getEmail());
		}
		// 로그인 안되있을 경우
		else {
			// 로그인 버튼이 있는 창 띄우기
			mLogoutView.setVisibility(View.GONE);
			mLoginView.setVisibility(View.VISIBLE);
		}
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.signin_loginButton:
				signIn(mIDField.getText().toString(), mPasswordField.getText().toString());
				return;
			case R.id.signin_logoutButton:
				signOut();
				return;
			case R.id.signin_signupButton:
				Intent intent = new Intent(this, SignupActivity.class);
				startActivity(intent);
				return;
			//네비게이션 버튼 클릭시 이동하는 인텐트
			case R.id.activity_signin_bottomBar_dashboardicon:
				startActivity(new Intent(this, MainTestActivity.class));
				// 애니메이션
				finish();
				overridePendingTransition(R.anim.push_right_in, R.anim.push_left_out);
				break;
			case R.id.activity_signin_bottomBar_listicon:
				startActivity(new Intent(this, RecyclerViewMoneyFlowFB.class));
				// 애니메이션
				finish();
				overridePendingTransition(R.anim.push_right_in, R.anim.push_left_out);
				break;
			case R.id.activity_signin_bottomBar_messengericon:
				startActivity(new Intent(this, MessengerChatRoomList.class));
				// 애니메이션
				finish();
				overridePendingTransition(R.anim.push_right_in, R.anim.push_left_out);
				break;
			default:
				return;
		}
	}
}
