package cn.abcdsxg.app.appJump.Activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;
import cn.abcdsxg.app.appJump.Base.BaseActivity;
import cn.abcdsxg.app.appJump.Data.Constant;
import cn.abcdsxg.app.appJump.Data.Utils.SpUtil;
import cn.abcdsxg.app.appJump.R;
import cn.abcdsxg.app.appJump.Service.GetAppInfoService;
import cn.abcdsxg.app.appJump.Service.TouchService;


/**
 * Author : 时小光
 * Email  : abcdsxg@gmail.com
 * Blog   : http://www.abcdsxg.cn
 * Date   : 16-11-15 14:10
 */

public class PreferenceSettingActivity extends BaseActivity {

    @Override
    public int getViewId() {
        return R.layout.fragment_setting;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Toolbar toolbar=(Toolbar)findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.Settings);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getFragmentManager().beginTransaction()
                .replace(R.id.frameLayout,new PreferenceSettingFragment()).commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case android.R.id.home:
                finish();
                break;

        }
        return super.onOptionsItemSelected(item);
    }

    public static class PreferenceSettingFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener,Preference.OnPreferenceClickListener{
        private ListPreference panelList;
        public static final String DEFAULT_PANEL="0";
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            PreferenceManager prefMgr = getPreferenceManager();
            prefMgr.setSharedPreferencesName("config");
            prefMgr.setSharedPreferencesMode(MODE_APPEND);
            prefMgr.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
            addPreferencesFromResource(R.xml.preference_setting);
            initPreference();
        }

        private void initPreference() {
            panelList=(ListPreference)findPreference(Constant.PANEL);
            initList();
            Preference addPanel=findPreference(Constant.ADD_PANEL);
            Preference editPanel=findPreference(Constant.EDIT_PANEL);
            addPanel.setOnPreferenceClickListener(this);
            editPanel.setOnPreferenceClickListener(this);
        }

        private void initList() {
            CharSequence[] entryKeys;
            CharSequence[] entryValues;
            int panelCount= SpUtil.getIntSp(getActivity().getApplication(),Constant.PANELCOUNT);
            //默认没有新面板的情况
            if(panelCount==-1) {
                entryKeys = new String[1];
                entryValues=new String[1];
                SpUtil.saveSp(getActivity().getApplicationContext(),Constant.PANELCOUNT,1);
                SpUtil.saveSp(getActivity().getApplicationContext(),Constant.PANEL+"1","默认面板");
            }else{
                entryKeys = new String[panelCount];
                entryValues=new String[panelCount];
                for (int i = 1; i < entryKeys.length; i++) {
                    entryKeys[i]=SpUtil.getStringSp(getActivity().getApplicationContext(),Constant.PANEL+String.valueOf(i+1));
                    entryValues[i]=String.valueOf(i+1);
                }
            }
            entryKeys[0]=getString(R.string.default_panel_text);
            entryValues[0]=DEFAULT_PANEL;
            panelList.setEntries(entryKeys);
            panelList.setEntryValues(entryValues);
            panelList.setValue(SpUtil.getStringSp(getActivity().getApplicationContext(),Constant.PANEL));

        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            Context context=getActivity().getApplicationContext();
            switch (key) {
                case Constant.SHOWCLSNAME:
                    if (sharedPreferences.getBoolean(key, false)) {
                        Intent intent = new Intent(context, GetAppInfoService.class);
                        context.startService(intent);
                    }
                    break;
                case Constant.MINDISTANT:
                    int minDistant = 0;
                    try {
                        minDistant = Integer.valueOf(sharedPreferences.getString(key, "20"));
                    } catch (Exception e) {
                        Toast.makeText(context, R.string.num_format_error, Toast.LENGTH_SHORT).show();
                    }
                    if (minDistant < 5 || minDistant > 80) {
                        Toast.makeText(context, R.string.area_error, Toast.LENGTH_SHORT).show();
                        SpUtil.saveSp(context, Constant.MINDISTANT, "20");
                    }else{
                        SpUtil.saveSp(context, Constant.MINDISTANT, sharedPreferences.getString(key, "20"));
                        context.startService(new Intent(context,TouchService.class));
                    }
                    break;
                case Constant.SLIDEPOS:
                case Constant.TOUCHSERVICE:
                    context.startService(new Intent(context,TouchService.class));
                    break;
                case Constant.SHOWBOUND:
                    Intent intent = new Intent(context, TouchService.class);
                    if(sharedPreferences.getBoolean(key, false)) {
                        AlertDialog dialog=new AlertDialog.Builder(getActivity())
                                .setMessage(R.string.set_bound_tip)
                                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialogInterface.dismiss();
                                    }
                                })
                                .create();
                        dialog.show();
                        intent.putExtra(Constant.SHOWBOUND, 1);
                    }
                    context.startService(intent);
                    break;
            }
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            Intent intent=new Intent(getActivity().getApplicationContext(),EditPanelActivity.class);
            switch (preference.getKey()){
                case Constant.ADD_PANEL:
                    intent.putExtra(Constant.PANEL,Constant.ADD_PANEL);
                    startActivity(intent);
                    break;
                case Constant.EDIT_PANEL:
                    intent.putExtra(Constant.PANEL,Constant.EDIT_PANEL);
                    startActivity(intent);
                    break;
            }
            return false;
        }
        @Override
        public void onResume() {
            super.onResume();
            if(panelList!=null) {
                initList();
            }
        }
    }

}
