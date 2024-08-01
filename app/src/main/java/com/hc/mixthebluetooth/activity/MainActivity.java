package com.hc.mixthebluetooth.activity;


import android.app.AlertDialog;
import android.content.Intent;

import android.os.Handler;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.app.hubert.guide.NewbieGuide;
import com.app.hubert.guide.model.GuidePage;
import com.app.hubert.guide.model.RelativeGuide;
import com.hc.basiclibrary.dialog.CommonDialog;
import com.hc.basiclibrary.permission.PermissionUtil;
import com.hc.basiclibrary.popupWindow.CommonPopupWindow;
import com.hc.basiclibrary.titleBasic.DefaultNavigationBar;
import com.hc.basiclibrary.viewBasic.BaseActivity;
import com.hc.bluetoothlibrary.DeviceModule;
import com.hc.mixthebluetooth.R;
import com.hc.mixthebluetooth.activity.single.HoldBluetooth;
import com.hc.mixthebluetooth.activity.tool.Analysis;
import com.hc.mixthebluetooth.customView.PopWindowMain;
import com.hc.mixthebluetooth.customView.dialog.CollectBluetooth;
import com.hc.mixthebluetooth.customView.dialog.HintHID;
import com.hc.mixthebluetooth.customView.dialog.PermissionHint;
import com.hc.mixthebluetooth.databinding.ActivityMainBinding;
import com.hc.mixthebluetooth.recyclerData.MainRecyclerAdapter;
import com.hc.mixthebluetooth.storage.Storage;

import java.util.ArrayList;
import java.util.List;

/*
  特别说明：HC蓝牙助手是广州汇承信息科技有限公司独自研发的手机APP，方便用户调试蓝牙模块。
  本软件提供代码和注释，免费给购买汇承蓝牙模块的用户学习和研究，但不能用于商业开发，
  最终解析权在广州汇承信息科技有限公司。
  :)
  **/

/**
 * @author 广州汇承信息科技有限公司
 * data: 2020-07-21
 * version: V1.3
 */
public class MainActivity extends BaseActivity<ActivityMainBinding> {

    private MainRecyclerAdapter mainRecyclerAdapter;

    private DefaultNavigationBar mTitle;

    private Storage mStorage;

    private final List<DeviceModule> mModuleArray = new ArrayList<>();
    private final List<DeviceModule> mFilterModuleArray = new ArrayList<>();

    private HoldBluetooth mHoldBluetooth;

    private int mStartDebug = 1;

    @Override
    public void initAll() {

        mStorage = new Storage(this);//sp存储

        //设置头部
        setTitle();

        //初始化单例模式中的蓝牙扫描回调
        initHoldBluetooth();

        //初始化权限
        initPermission();

        //初始化View
        initView();

        //初始化下拉刷新
        initRefresh();

        //设置RecyclerView的Item的点击事件
        setRecyclerListener();
    }

    @Override
    protected ActivityMainBinding getViewBinding() {
        return ActivityMainBinding.inflate(getLayoutInflater());
    }

    private void initHoldBluetooth() {
        mHoldBluetooth = HoldBluetooth.getInstance();
        final HoldBluetooth.UpdateList updateList = new HoldBluetooth.UpdateList() {
            @Override
            public void update(boolean isStart,DeviceModule deviceModule) {

                if (isStart && deviceModule == null){//更新距离值
                    mainRecyclerAdapter.notifyDataSetChanged();
                    return;
                }

                if (isStart){
                    setMainBackIcon();
                    mModuleArray.add(deviceModule);
                    addFilterList(deviceModule,true);
                }else {
                    mTitle.updateLoadingState(false);
                }
            }

            @Override
            public void updateMessyCode(boolean isStart, DeviceModule deviceModule) {
                for(int i= 0; i<mModuleArray.size();i++){
                    if (mModuleArray.get(i).getMac().equals(deviceModule.getMac())){
                        mModuleArray.remove(mModuleArray.get(i));
                        mModuleArray.add(i,deviceModule);
                        upDateList();
                        break;
                    }
                }
            }
        };
        mHoldBluetooth.initHoldBluetooth(MainActivity.this,updateList);
    }

    private void initView() {
        setMainBackIcon();
        mainRecyclerAdapter = new MainRecyclerAdapter(this,mFilterModuleArray,R.layout.item_recycler_main);
        viewBinding.mainRecycler.setLayoutManager(new LinearLayoutManager(this));
        viewBinding.mainRecycler.setAdapter(mainRecyclerAdapter);
    }

    //初始化下拉刷新
    private void initRefresh() {
        //设置刷新监听器
        viewBinding.mainSwipe.setOnRefreshListener(() -> {
            viewBinding.mainSwipe.setRefreshing(false);
            refresh();
        });
    }

    //刷新的具体实现
    private void refresh(){
        popDialog();
        if (mHoldBluetooth.scan(mStorage.getData(PopWindowMain.BLE_KEY))){
            mModuleArray.clear();
            mFilterModuleArray.clear();
            mTitle.updateLoadingState(true);
            mainRecyclerAdapter.notifyDataSetChanged();
        }
    }

    //根据条件过滤列表，并选择是否更新列表
    private void addFilterList(DeviceModule deviceModule,boolean isRefresh){
        if (mStorage.getData(PopWindowMain.NAME_KEY) && deviceModule.getName().equals("N/A")) return;

        if (mStorage.getData(PopWindowMain.BLE_KEY) && !deviceModule.isBLE()) return;

        if ((mStorage.getData(PopWindowMain.FILTER_KEY) || mStorage.getData(PopWindowMain.CUSTOM_KEY))
         && !deviceModule.isHcModule(mStorage.getData(PopWindowMain.CUSTOM_KEY),mStorage.getDataString(PopWindowMain.DATA_KEY))){
            return;
        }
        deviceModule.isCollectName(MainActivity.this);
        mFilterModuleArray.add(deviceModule);
        if (isRefresh) mainRecyclerAdapter.notifyDataSetChanged();
    }

    //设置头部
    private void setTitle() {
        mTitle = new DefaultNavigationBar
                .Builder(this, findViewById(R.id.main_name))
                .setLeftText("HC蓝牙助手", CommonPopupWindow.dip2px(this,20))
                .hideLeftIcon()
                .setRightIcon()
                .setLeftClickListener(v -> {
                    if (mStartDebug % 4 ==0) startActivity(DebugActivity.class);
                    mStartDebug++;
                })
                .setRightClickListener(v -> {
                    setPopWindow(v);
                    mTitle.updateRightImage(true);
                })
                .builer();
    }



    //头部下拉窗口
    private void setPopWindow(View v){
        new PopWindowMain(v, MainActivity.this, resetEngine -> {//弹出窗口销毁的回调
           upDateList();
           mTitle.updateRightImage(false);
           if (resetEngine){//更换搜索引擎，重新搜索
               mHoldBluetooth.stopScan();
               refresh();
           }
        });
    }

    //设置点击事件
    private void setRecyclerListener() {
        mainRecyclerAdapter.setOnItemClickListener((position, view) -> {
            log("viewId:"+view.getId()+" item_main_icon:"+R.id.item_main_icon);
            if (view.getId() == R.id.item_main_icon){
                setCollectWindow(position);//收藏窗口
            }else {
                if (mFilterModuleArray.get(position).getIBeacon() != null){
                    toastShort("此设备目前状态不可连接");
                    return;
                }
                mHoldBluetooth.setDevelopmentMode(MainActivity.this);//设置是否进入开发模式
                mHoldBluetooth.connect(mFilterModuleArray.get(position));
                startActivity(CommunicationActivity.class);
            }
        });
    }

    //收藏窗口
    private void setCollectWindow(int position) {
        log("弹出窗口..");
        CommonDialog.Builder collectBuilder = new CommonDialog.Builder(MainActivity.this);
        collectBuilder.setView(R.layout.hint_collect_vessel).fullWidth().loadAnimation().create().show();
        CollectBluetooth collectBluetooth = collectBuilder.getView(R.id.hint_collect_vessel_view);
        collectBluetooth.setBuilder(collectBuilder).setDevice(mFilterModuleArray.get(position)).setCallback(this::upDateList);
    }

    //更新列表
    private void upDateList(){
        mFilterModuleArray.clear();
        for (DeviceModule deviceModule : mModuleArray) {
            addFilterList(deviceModule,false);
        }
        mainRecyclerAdapter.notifyDataSetChanged();
        setMainBackIcon();
    }

    //设置列表的背景图片是否显示
    private void setMainBackIcon(){
        if (mFilterModuleArray.size() == 0){
            viewBinding.mainBackNot.setVisibility(View.VISIBLE);
        }else {
            viewBinding.mainBackNot.setVisibility(View.GONE);
        }
    }

    //扫描弹出提醒框
    private void popDialog(){
        if (mStorage != null && mStorage.getFirstTime()) {
            CommonDialog.Builder hidBuilder = new CommonDialog.Builder(MainActivity.this);
            CommonDialog dialog = hidBuilder.setView(R.layout.hint_hid_vessel)
                    .loadAnimation().fullWidth().setCancelable(false).create();
            HintHID hintHID = hidBuilder.getView(R.id.hint_hid_vessel_view);
            hintHID.setBuilder(hidBuilder);
            hintHID.setOnDismissListener(() -> {
                log("dialog is cancel");
                setGuide();
            });
            if (hintHID.isShow()) dialog.show();
        }
    }

    /**
     * 设置引导界面
     */
    private void setGuide(){
        NewbieGuide.with(this)
                .setLabel("guide")
                .anchor(getWindow().getDecorView())
                .addGuidePage(GuidePage.newInstance()
                .addHighLight(mTitle.getView(R.id.right_icon),
                        new RelativeGuide(R.layout.guide_page_main, Gravity.START))
                .setOnLayoutInflatedListener((view, controller) -> {
                    String data = "点击此处，可切换为专门扫描BLE蓝牙设备模式👉";
                    TextView textView = view.findViewById(R.id.guide_page_text);
                    if (textView != null) textView.setText(data);
                }))
                .show();
    }

    //初始化位置权限
    private void initPermission(){
        PermissionUtil.requestEach(MainActivity.this, new PermissionUtil.OnPermissionListener() {
            @Override
            public void onSucceed() {
                //授权成功后打开蓝牙
                log("申请成功");
                new Handler().postDelayed(() -> {
                    if (mHoldBluetooth.bluetoothState()){
                        if (Analysis.isOpenGPS(MainActivity.this)) {
                            refresh();
                        }else {
                            startLocation();
                        }
                    }
                },1000);

            }
            @Override
            public void onFailed(boolean showAgain) {
                logError("失败: "+showAgain);
                CommonDialog.Builder permissionBuilder = new CommonDialog.Builder(MainActivity.this);
                permissionBuilder.setView(R.layout.hint_permission_vessel).fullWidth().setCancelable(false).loadAnimation().create().show();
                PermissionHint permissionHint = permissionBuilder.getView(R.id.hint_permission_vessel_view);
                permissionHint.setBuilder(permissionBuilder).setPermission(showAgain).setCallback(permission -> {
                    if (permission) {
                        initPermission();
                    }else {
                        finish();
                    }
                });
            }
        }, PermissionUtil.LOCATION);
    }

    //开启位置权限
    private void startLocation(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this,android.R.style.Theme_DeviceDefault_Light_Dialog_Alert);
        builder.setTitle("提示")
                .setMessage("请前往打开手机的位置权限!")
                .setCancelable(false)
                .setPositiveButton("确定", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivityForResult(intent, 10);
                }).show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //退出这个界面，或是返回桌面时，停止扫描
        mHoldBluetooth.stopScan();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        logWarn("关闭MainActivity...");
    }
}