package com.hc.mixthebluetooth.fragment;

import android.content.Context;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.hc.basiclibrary.viewBasic.BaseFragment;
import com.hc.basiclibrary.viewBasic.manage.BaseFragmentManage;
import com.hc.bluetoothlibrary.DeviceModule;
import com.hc.bluetoothlibrary.tootl.ModuleParameters;
import com.hc.mixthebluetooth.R;
import com.hc.mixthebluetooth.activity.single.FragmentParameter;
import com.hc.mixthebluetooth.activity.single.StaticConstants;
import com.hc.mixthebluetooth.activity.tool.Analysis;
import com.hc.mixthebluetooth.databinding.FragmentCustomBinding;
import com.hc.mixthebluetooth.recyclerData.FragmentMessAdapter;
import com.hc.mixthebluetooth.recyclerData.itemHolder.FragmentMessageItem;
import com.hc.mixthebluetooth.storage.Storage;

import java.util.ArrayList;
import java.util.List;

public class FragmentCustom extends BaseFragment<FragmentCustomBinding> {

    private FragmentMessAdapter mAdapter;

    private final List<FragmentMessageItem> mDataList = new ArrayList<>();

    private DeviceModule module;

    private FragmentParameter mFragmentParameter;

    private int mFragmentHeight;

    private BaseFragmentManage mFragmentManage;

    private Storage mStorage;

    @Override
    protected void initAll(View view,Context context) {
        mStorage = new Storage(context);
        mFragmentParameter = FragmentParameter.getInstance();
        initData();
        initRecycler();
        initFragment();
        viewBinding.customFragmentDirection.setState(true);
        viewBinding.customFragmentPullImage.setTag(R.drawable.pull_down);
        new Handler().postDelayed(this::setViewHeight,500);
    }


    @Override
    protected void updateState(String sign, Object o) {
        if (module == null &&  StaticConstants.FRAGMENT_STATE_DATA.equals(sign)) {
            module = (DeviceModule) o;
        }
        if (o instanceof Object[] && StaticConstants.FRAGMENT_STATE_DATA.equals(sign) && viewBinding.customFragmentShowReadCheck.isChecked()) {
            Object[] objects = (Object[]) o;
            if (objects.length<2) return;
            byte[] data = ((byte[]) objects[1]).clone();
            addListData(data);
            mAdapter.notifyDataSetChanged();
            viewBinding.customFragmentRecycler.smoothScrollToPosition(mDataList.size());
        }
    }

    /**
     * 根据数据末尾是否换行，来判定数据添加到{@link #mDataList}的上一个元素或重新创建一个元素
     * @param data 接收到的数据
     */
    private void addListData(byte[] data) {

        if(!ModuleParameters.isCheckNewline()){//不检查换行符
            mDataList.add(new FragmentMessageItem(Analysis.getByteToString(data,mFragmentParameter.getCodeFormat(getContext()),
                    viewBinding.customFragmentReadCheck.isChecked(), false),  null, false, module, false));
            return;
        }

        boolean newline = data[data.length-1] == 10 && data[data.length-2] == 13;
        String dataString = Analysis.getByteToString(data,mFragmentParameter.getCodeFormat(getContext()),
                viewBinding.customFragmentReadCheck.isChecked(), newline);
        if (mDataList.size()>0 && mDataList.get(mDataList.size()-1).isAddible()){//数组里最后一个元素没有换行符可以添加数据
            //log("数据合并一次: "+mDataList.get(mDataList.size()-1).isAddible());
            mDataList.get(mDataList.size()-1).addData(dataString,null);
            mDataList.get(mDataList.size()-1).setDataEndNewline(newline);
        }else {//数组最后一个元素有换行符且已处理，创建一个新的元素加载数据并添加至数组最后
            //log("创建一个新的Item存储: newline is "+newline);
            mDataList.add(new FragmentMessageItem(dataString,  null, false, module, false));
            mDataList.get(mDataList.size()-1).setDataEndNewline(newline);
        }
    }

    private void initRecycler(){
        mAdapter = new FragmentMessAdapter(getContext(),mDataList,R.layout.item_message_fragment);
        viewBinding.customFragmentRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        viewBinding.customFragmentRecycler.setAdapter(mAdapter);
        viewBinding.customFragmentShowReadCheck.setChecked(mStorage.getDataCheckState());
    }

    private void initFragment() {
        mFragmentManage = new BaseFragmentManage(R.id.custom_fragment,getActivity());
        mFragmentManage.addFragment(0,new FragmentCustomGroup());
        mFragmentManage.addFragment(1,new FragmentCustomDirection());
        mFragmentManage.showFragment(1);
    }

    private void setViewHeight() {//动态设置fragment的高度
        viewBinding.customFragment.post(() -> {
            mFragmentHeight = viewBinding.customFragment.getHeight();
            ViewGroup.LayoutParams params=viewBinding.customFragment.getLayoutParams();
            params.height= mFragmentHeight;
            logError("height is "+mFragmentHeight);
            viewBinding.customFragment.setLayoutParams(params);
        });
    }


    public void onClickView(View view){
        if (isCheck(viewBinding.customFragmentPullImage)) setViewAnimation();
        if (isCheck(viewBinding.customFragmentShowReadCheck) || isCheck(viewBinding.customFragmentShowReadText)){
            viewBinding.customFragmentShowReadCheck.toggle();
            mStorage.saveCheckShowDataState(viewBinding.customFragmentNewlineCheck.isChecked());
        }else if (isCheck(viewBinding.customFragmentGroup)){
            viewBinding.customFragmentGroup.setState(true);
            viewBinding.customFragmentDirection.setState(false);
            mFragmentManage.showFragment(0);
        }else if (isCheck(viewBinding.customFragmentDirection)){
            viewBinding.customFragmentGroup.setState(false);
            viewBinding.customFragmentDirection.setState(true);
            mFragmentManage.showFragment(1);
        }else if (isCheck(viewBinding.customFragmentReadCheck) || isCheck(viewBinding.customFragmentReadHex)){
            viewBinding.customFragmentReadCheck.toggle();
        }else if (isCheck(viewBinding.customFragmentNewlineCheck) || isCheck(viewBinding.customFragmentNewlineText)){
            viewBinding.customFragmentNewlineCheck.toggle();
            sendDataToActivity(StaticConstants.FRAGMENT_CUSTOM_NEWLINE,viewBinding.customFragmentNewlineCheck.isChecked());
        }else if (isCheck(viewBinding.customFragmentEmpty)) {
            mDataList.clear();
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected FragmentCustomBinding getViewBinding() {
        return FragmentCustomBinding.inflate(getLayoutInflater());
    }

    private void setViewAnimation() {
        log("Tag is "+viewBinding.customFragmentPullImage.getTag()+" id is "+R.drawable.pull_down);
        if (Integer.parseInt( viewBinding.customFragmentPullImage.getTag().toString()) == R.drawable.pull_down){
            viewBinding.customFragmentPullImage.setTag(R.drawable.pull_up);
            viewBinding.customFragmentPullImage.setImageResource(R.drawable.pull_up);
            Analysis.changeViewHeightAnimatorStart(viewBinding.customFragment,mFragmentHeight,0);
        }else {
            viewBinding.customFragmentPullImage.setTag(R.drawable.pull_down);
            viewBinding.customFragmentPullImage.setImageResource(R.drawable.pull_down);
            Analysis.changeViewHeightAnimatorStart(viewBinding.customFragment,0,mFragmentHeight);
        }
    }

    private void initData() {
        View[] viewArray = {viewBinding.customFragmentPullImage,viewBinding.customFragmentShowReadCheck,viewBinding.customFragmentShowReadText,
                viewBinding.customFragmentGroup,viewBinding.customFragmentDirection,viewBinding.customFragmentReadCheck,viewBinding.customFragmentReadHex,
                viewBinding.customFragmentNewlineCheck,viewBinding.customFragmentNewlineText,viewBinding.customFragmentEmpty};
        bindOnClickListener(viewArray);
        subscription(StaticConstants.FRAGMENT_STATE_DATA);
    }
}
