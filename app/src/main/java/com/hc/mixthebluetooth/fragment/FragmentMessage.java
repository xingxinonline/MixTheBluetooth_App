package com.hc.mixthebluetooth.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import com.hc.basiclibrary.dialog.CommonDialog;
import com.hc.basiclibrary.recyclerAdapterBasic.FastScrollLinearLayoutManager;
import com.hc.basiclibrary.titleBasic.DefaultNavigationBar;
import com.hc.basiclibrary.viewBasic.BaseFragment;
import com.hc.bluetoothlibrary.DeviceModule;
import com.hc.bluetoothlibrary.tootl.ModuleParameters;
import com.hc.mixthebluetooth.R;
import com.hc.mixthebluetooth.activity.single.FragmentParameter;
import com.hc.mixthebluetooth.activity.single.HoldBluetooth;
import com.hc.mixthebluetooth.activity.single.StaticConstants;
import com.hc.mixthebluetooth.activity.tool.Analysis;
import com.hc.mixthebluetooth.customView.PopWindowFragment;
import com.hc.mixthebluetooth.customView.dialog.InvalidHint;
import com.hc.mixthebluetooth.databinding.FragmentMessageBinding;
import com.hc.mixthebluetooth.recyclerData.FragmentMessAdapter;
import com.hc.mixthebluetooth.recyclerData.itemHolder.FragmentMessageItem;
import com.hc.mixthebluetooth.storage.Storage;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class FragmentMessage extends BaseFragment<FragmentMessageBinding> {

    private DefaultNavigationBar mTitle;//activity的头部

    private FragmentMessAdapter mAdapter;

    private final List<FragmentMessageItem> mDataList = new ArrayList<>();

    private DeviceModule module = null;

    private Storage mStorage;

    private FragmentParameter mFragmentParameter;

    private int mCacheByteNumber = 0;//mCacheByteNumber: 缓存的字节数

    private boolean isShowMyData,isSendHex,isShowTime,isReadHex,isAutoClear,isSendNewline;//弹出窗的六个选择

    private int mFoldLayoutHeight = 0;

    private final Handler mTimeHandler = new Handler();

    private final Timer mTimer = new Timer();//循环发送的具体线程
    private TimerTask mTimerTask;//循环发送的定时器


    @Override
    public void initAll(View view, Context context) {
        mStorage = new Storage(context);
        mFragmentParameter = FragmentParameter.getInstance();
        setListState();
        initRecycler();
        initEditView();
        initFoldLayout();
        initData();
    }


    @SuppressLint("SetTextI18n")
    @Override
    protected void updateState(String sign, Object o) {
        switch (sign){
            case StaticConstants.FRAGMENT_STATE_DATA:
                //logWarn("获取module信息...");
                byte[] data = null;
                if(o instanceof Object[]) {
                    Object[] objects = (Object[]) o;
                    data = objects.length > 1 ? ((byte[]) objects[1]).clone() : null;
                }else if (o instanceof DeviceModule) module = (DeviceModule) o;

                if (data != null) {
                    addListData(data);
                    mAdapter.notifyDataSetChanged();
                    viewBinding.recyclerMessageFragment.smoothScrollToPosition(mDataList.size());
                    viewBinding.sizeReadMessageFragment.setText(String.valueOf(Integer.parseInt(viewBinding.sizeReadMessageFragment.getText().toString())+data.length));
                    setClearRecycler(data.length);//判断是否清屏（清除缓存）
                }
                break;
            case StaticConstants.FRAGMENT_STATE_NUMBER:
                viewBinding.sizeSendMessageFragment.setText(String.valueOf(Integer.parseInt(viewBinding.sizeSendMessageFragment.getText().toString())+((int) o)));
                //setUnsentNumberTv();
                break;
            case StaticConstants.FRAGMENT_STATE_SEND_SEND_TITLE:
                mTitle = (DefaultNavigationBar) o;
                break;
            case StaticConstants.FRAGMENT_STATE_SERVICE_VELOCITY:
                int velocity = (int) o;
                viewBinding.readVelocityMessageFragment.setText("速度: "+velocity+"B/s");
                break;
            case StaticConstants.FRAGMENT_STATE_1:
                viewBinding.readHintMessageFragment.setVisibility(View.VISIBLE);
                break;
            case StaticConstants.FRAGMENT_STATE_2:
                viewBinding.readHintMessageFragment.setVisibility(View.GONE);
                break;
            case StaticConstants.FRAGMENT_STATE_STOP_LOOP_SEND:
                if(mTimerTask != null) {
                    viewBinding.sendMessageFragment.setText("发送");
                    mTimerTask.cancel();
                    mTimerTask = null;
                    logWarn("Fragment Message 停止发送");
                    HoldBluetooth.getInstance().stopSend(module, null);
                }
                break;
        }
    }




    public void onClickView(View view){
        if (isCheck(viewBinding.sendMessageFragment)) setSendData();
        if (isCheck(viewBinding.foldSwitchMessageFragment)) setFoldLayout();
        if (isCheck(viewBinding.clearMessageFragment)) {
            if (viewBinding.sendMessageFragment.getText().toString().equals("发送")) {
                viewBinding.editMessageFragment.setText("");
            }else {
                toastShort("连续发送中，不能清除发送区的数据");
            }
        }else if (isCheck(viewBinding.pullMessageFragment)){
            viewBinding.pullMessageFragment.setImageResource(R.drawable.pull_up);
            new PopWindowFragment(view, getActivity(), new PopWindowFragment.DismissListener() {
                @Override
                public void onDismissListener() {
                    viewBinding.pullMessageFragment.setImageResource(R.drawable.pull_down);
                    setListState();
                }

                @Override
                public void clearRecycler() {
                    mDataList.clear();
                    viewBinding.sizeReadMessageFragment.setText(String.valueOf(0));
                    viewBinding.sizeSendMessageFragment.setText(String.valueOf(0));
                    mCacheByteNumber = 0;
                    mAdapter.notifyDataSetChanged();
                }
            });

        }else if(isCheck(viewBinding.loopTextMessageFragment) || isCheck(viewBinding.loopCheckMessageFragment)){
            if (Integer.parseInt(viewBinding.loopTimeMessageFragment.getText().toString()) <10){
                toastShort("设置时间必须大于10，不然速度过快无法发送");
                return;
            }
            if (viewBinding.loopCheckMessageFragment.isChecked() && viewBinding.sendMessageFragment.getText().toString().equals("停止")) setSendData();
            viewBinding.loopCheckMessageFragment.toggle();
        }
    }

    @Override
    protected FragmentMessageBinding getViewBinding() {
        return FragmentMessageBinding.inflate(getLayoutInflater());
    }

    private void setSendData() {
        if (!mTitle.getParams().mRightText.equals("已连接")){
            toastShort("当前状态不可以发送数据");
            return;
        }
        if (viewBinding.editMessageFragment.getText().toString().equals("")){
            toastShort("不能发送空数据");
            return;
        }
        if (!viewBinding.loopCheckMessageFragment.isChecked()) {
            String data = viewBinding.editMessageFragment.getText().toString();
            if (isSendNewline) data += isSendHex?"0D0A":"\r\n";
            if(isSendHex) data = data.replaceAll(" ","");
            sendData(new FragmentMessageItem(isSendHex, Analysis.getBytes(data,mFragmentParameter.getCodeFormat(getContext()), isSendHex), isShowTime ? Analysis.getTime() : null, true, module, isShowMyData));
            dataScreening(viewBinding.editMessageFragment.getText().toString());
        }else {
            try {
                Integer.parseInt(viewBinding.loopTimeMessageFragment.getText().toString());
            }catch (Exception e){
                e.printStackTrace();
                toastShort("时间输入不规范");
                return;
            }
            if (viewBinding.sendMessageFragment.getText().toString().equals("发送")){
                viewBinding.sendMessageFragment.setText("停止");
                final int time = Integer.parseInt(viewBinding.loopTimeMessageFragment.getText().toString());
                mTimerTask = new TimerTask() {
                    @Override
                    public void run() {
                        String data = viewBinding.editMessageFragment.getText().toString();
                        if (isSendNewline) data += isSendHex?"0D0A":"\r\n";
                        if (isSendHex) data = data.replaceAll(" ","");
                        final String sendData = data;
                        mTimeHandler.post(()-> sendData(new FragmentMessageItem(isSendHex, Analysis.getBytes(sendData,mFragmentParameter.getCodeFormat(getContext()), isSendHex), isShowTime ? Analysis.getTime() : null, true, module, isShowMyData)));
                    }
                };
                mTimer.schedule(mTimerTask,0,time);
            }else {
                viewBinding.sendMessageFragment.setText("发送");
                HoldBluetooth.getInstance().stopSend(module,null);
                mTimerTask.cancel();
                mTimerTask = null;
            }
        }
    }

    //弹出提示框，警告AT指令设置无效
    private void dataScreening(String data) {
        String str = "AT+";
        if (data.length()<str.length()) return;
        String temp = data.substring(0,str.length());
        if (temp.equals(str) && mStorage.getInvalidAT()){
            CommonDialog.Builder invalidAtBuilder = new CommonDialog.Builder(getContext());
            invalidAtBuilder.setView(R.layout.hint_invalid_vessel).fullWidth().loadAnimation().create().show();
            InvalidHint invalidHint = invalidAtBuilder.getView(R.id.hint_invalid_vessel_view);
            invalidHint.setBuilder(invalidAtBuilder);
        }
    }

    private void setFoldLayout() {
        if ((int)viewBinding.foldSwitchMessageFragment.getTag() == R.drawable.pull_down){
            viewBinding.foldSwitchMessageFragment.setImageResource(R.drawable.pull_up);
            viewBinding.foldSwitchMessageFragment.setTag(R.drawable.pull_up);
            Analysis.changeViewHeightAnimatorStart(viewBinding.foldLayoutMessageFragment,mFoldLayoutHeight,0);
        }else{
            viewBinding.foldSwitchMessageFragment.setImageResource(R.drawable.pull_down);
            viewBinding.foldSwitchMessageFragment.setTag(R.drawable.pull_down);
            Analysis.changeViewHeightAnimatorStart(viewBinding.foldLayoutMessageFragment,0,mFoldLayoutHeight);
        }
    }

    private void setListState() {
        isShowMyData = mStorage.getData(PopWindowFragment.KEY_DATA);
        isShowTime = mStorage.getData(PopWindowFragment.KEY_TIME);
        isSendHex = mStorage.getData(PopWindowFragment.KEY_HEX_SEND);
        isReadHex = mStorage.getData(PopWindowFragment.KEY_HEX_READ);
        isAutoClear = mStorage.getData(PopWindowFragment.KEY_CLEAR);
        isSendNewline = mStorage.getData(PopWindowFragment.KEY_NEWLINE);
        if (isSendHex && viewBinding.editMessageFragment.getHint().toString().trim().equals("任意字符")){
            viewBinding.editMessageFragment.setHint("只可以输入16进制数据");
            viewBinding.editMessageFragment.setText(Analysis.changeHexString(true,viewBinding.editMessageFragment.getText().toString().trim()));
        }else if (!isSendHex && viewBinding.editMessageFragment.getHint().toString().trim().equals("只可以输入16进制数据")){
            viewBinding.editMessageFragment.setHint("任意字符");
            viewBinding.editMessageFragment.setText(Analysis.changeHexString(false,viewBinding.editMessageFragment.getText().toString().trim()));
        }
    }

    private void setClearRecycler(int readNumber) {
        mCacheByteNumber += readNumber;
        if (isAutoClear){//开启清除缓存
            if (mCacheByteNumber>400000){//只缓存400K
                mDataList.clear();
                mAdapter.notifyDataSetChanged();
                mCacheByteNumber = 0;
            }
        }
    }

    /**
     * 根据数据末尾是否换行，来判定数据添加到{@link #mDataList}的上一个元素或重新创建一个元素
     * @param data 接收到的数据
     */
    private void addListData(byte[] data) {

        if (!ModuleParameters.isCheckNewline()){//不需要检查换行
            mDataList.add(new FragmentMessageItem( Analysis.getByteToString(data,mFragmentParameter.getCodeFormat(getContext()),
                    isReadHex, false), isShowTime ? Analysis.getTime() : null, false, module, isShowMyData));
            return;
        }

        boolean newline = data[data.length-1] == 10 && data[data.length-2] == 13;
        String dataString = Analysis.getByteToString(data,mFragmentParameter.getCodeFormat(getContext()),
                isReadHex, newline);
        if (mDataList.size()>0 && mDataList.get(mDataList.size()-1).isAddible()){//数组里最后一个元素没有换行符，可以添加数据
            logWarn("数据合并一次...");
            mDataList.get(mDataList.size()-1).addData(dataString,isShowTime?Analysis.getTime():null);
            mDataList.get(mDataList.size()-1).setDataEndNewline(newline);
        }else {//数组元素最后一个元素有换行符，且已经过处理，重新创建一个元素添加数据并放至最后
            logWarn("创建一个新的Item存储: newline is "+newline);
            mDataList.add(new FragmentMessageItem(dataString, isShowTime ? Analysis.getTime() : null, false, module, isShowMyData));
            mDataList.get(mDataList.size()-1).setDataEndNewline(newline);//填入是否有换行符
        }
    }

    private void sendData(FragmentMessageItem item) {
        sendDataToActivity(StaticConstants.DATA_TO_MODULE,item);
        //logWarn("发送数据");
        if (isShowMyData) {
            mDataList.add(item);
            mAdapter.notifyDataSetChanged();
            viewBinding.recyclerMessageFragment.smoothScrollToPosition(mDataList.size());
        }
    }

    private void initRecycler(){
        mAdapter = new FragmentMessAdapter(getContext(),mDataList,R.layout.item_message_fragment);
        viewBinding.recyclerMessageFragment.setLayoutManager(new FastScrollLinearLayoutManager(getContext()));
        viewBinding.recyclerMessageFragment.setAdapter(mAdapter);
    }


    private void initEditView() {
        viewBinding.editMessageFragment.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                log("charSequence is "+charSequence+" start is "+start+" before is "+before+" count is "+count);
                if (isSendHex) Analysis.setHex(charSequence.toString(),start,before,count,viewBinding.editMessageFragment);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    private void initFoldLayout() {
        viewBinding.foldSwitchMessageFragment.setTag(R.drawable.pull_down);
        viewBinding.loopTimeMessageFragment.setText(String.valueOf(500));
        viewBinding.foldLayoutMessageFragment.post(() -> mFoldLayoutHeight = viewBinding.foldLayoutMessageFragment.getHeight());
    }

    private void initData() {
        subscription(StaticConstants.FRAGMENT_STATE_DATA,StaticConstants.FRAGMENT_STATE_NUMBER,
                StaticConstants.FRAGMENT_STATE_SEND_SEND_TITLE,StaticConstants.FRAGMENT_STATE_SERVICE_VELOCITY,
                StaticConstants.FRAGMENT_STATE_1,StaticConstants.FRAGMENT_STATE_2,StaticConstants.FRAGMENT_STATE_STOP_LOOP_SEND);
        bindOnClickListener(viewBinding.sendMessageFragment,viewBinding.clearMessageFragment,viewBinding.pullMessageFragment,
                viewBinding.foldSwitchMessageFragment,viewBinding.loopCheckMessageFragment,viewBinding.loopTextMessageFragment);
        viewBinding.editMessageFragment.setText(mStorage.getSaveInputData());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mStorage.saveInputData(viewBinding.editMessageFragment.getText().toString().trim());
        HoldBluetooth.getInstance().stopSend(module,null);
        mTimer.cancel();
    }

}