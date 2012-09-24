package info.staticfree.android.dearfutureself;

import info.staticfree.android.dearfutureself.content.ImportExportException;
import info.staticfree.android.dearfutureself.content.MessageUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.support.v4.content.ModernAsyncTask;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.ActionBarSherlock;

public class ImportExport extends Activity implements OnClickListener, OnItemClickListener {

    public static final String TAG = ImportExport.class.getSimpleName();

    private final ActionBarSherlock mSherlock = ActionBarSherlock.wrap(this);

    public static final String EXPORT_PREFIX = "dearfutureself-",
        EXPORT_SUFFIX = ".json";

    private static final Pattern DATE_STAMPED_FILE = Pattern.compile(Pattern.quote(EXPORT_PREFIX) + "(\\d+)"+ Pattern.quote(EXPORT_SUFFIX));

    private ListView mBackupList;
    private BackupAdapter mBackupAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSherlock.setContentView(R.layout.import_export);

        findViewById(R.id.export_msgs).setOnClickListener(this);
        mBackupList = (ListView) findViewById(R.id.backups);
        final File externalDir = getExternalFilesDir(null);
        if (externalDir == null) {
            Toast.makeText(
                    this,
                    "At the moment, Import/Export requires an external files folder. If you have an SD card, please mount it and try again.",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        mBackupAdapter = new BackupAdapter(this, android.R.layout.simple_list_item_1, externalDir);
        mBackupList.setAdapter(mBackupAdapter);
        mBackupList.setOnItemClickListener(this);
        mBackupAdapter.reload();


    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){

        case R.id.export_msgs:
                new ExportTask().execute(new File(getExternalFilesDir(null), EXPORT_PREFIX
                        + System.currentTimeMillis()
                        + EXPORT_SUFFIX));
            break;
        }

    }

    private static class BackupAdapter implements ListAdapter {

        private List<BackupItem> mBackupItems = new ArrayList<BackupItem>();

        private final Context mContext;
        private final LayoutInflater mInflater;

        private final DataSetObservable mObservable = new DataSetObservable();

        private final File mBaseDir;

        private final int mLayout;

        public BackupAdapter(Context context, int layoutId, File baseDir) {
            super();
            mContext = context;

            mBaseDir = baseDir;
            mLayout = layoutId;
            mInflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
        }

        public void reload(){
            final File[] files = mBaseDir.listFiles(new FileFilter() {

                @Override
                public boolean accept(File pathname) {
                    return pathname.getName().startsWith(EXPORT_PREFIX);
                }
            });

            final BackupItem[] backupItems = new BackupItem[files.length];

            for (int i = 0; i < files.length; i++){
                long time = 0;
                final Matcher m = DATE_STAMPED_FILE.matcher(files[i].getName());
                if (m.matches()){
                    time = Long.valueOf(m.group(1));
                }

                backupItems[i] = new BackupItem(mContext, files[i], time);
            }

            Arrays.sort(backupItems, new Comparator<BackupItem>() {

                @Override
                public int compare(BackupItem lhs, BackupItem rhs) {
                    return Long.valueOf(rhs.backupDate).compareTo(lhs.backupDate);
                }
            });
            mBackupItems = Arrays.asList(backupItems);
            mObservable.notifyChanged();
        }

        @Override
        public int getCount() {
            return mBackupItems.size();
        }

        @Override
        public BackupItem getItem(int position) {
            return mBackupItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getItemViewType(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null){
                convertView = mInflater.inflate(mLayout, parent, false);
            }

            ((TextView)convertView.findViewById(android.R.id.text1)).setText(getItem(position).toString());

            return convertView;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public boolean isEmpty() {
            return mBackupItems.size() == 0;
        }

        @Override
        public void registerDataSetObserver(DataSetObserver observer) {
            mObservable.registerObserver(observer);
        }

        @Override
        public void unregisterDataSetObserver(DataSetObserver observer) {
            mObservable.unregisterObserver(observer);
        }

        @Override
        public boolean areAllItemsEnabled() {
            return true;
        }

        @Override
        public boolean isEnabled(int position) {
            return true;
        }
    }

    private static class BackupItem {
        private final Context mContext;

        public BackupItem(Context context, File path, long backupDate) {
            mContext= context;
            this.path = path;
            this.backupDate = backupDate;
        }

        File path;
        long backupDate;

        @Override
        public String toString() {
            return (String) DateUtils.getRelativeDateTimeString(mContext, backupDate,
                    DateUtils.SECOND_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, 0);
        }
    }

    public class ExportTask extends ModernAsyncTask<File, Long, Boolean> {

        private ProgressDialog progress;

        @Override
        protected void onPreExecute() {
            progress = new ProgressDialog(ImportExport.this);
            progress.setMessage("Exporting messages...");
            progress.setIndeterminate(true);
            progress.setCancelable(true);

            progress.setOnCancelListener(new OnCancelListener() {

                @Override
                public void onCancel(DialogInterface dialog) {
                    ExportTask.this.cancel(true);
                }
            });
        }

        @Override
        protected Boolean doInBackground(File... dest) {
            try {
                final File destination = dest[0];
                destination.getParentFile().mkdirs();

                MessageUtils.exportJson(ImportExport.this, destination.getAbsolutePath());
                Log.i(TAG, "Exported to " + destination.getAbsolutePath());
            } catch (final FileNotFoundException e) {
                Log.e(TAG, "error exporting", e);
                return false;
            } catch (final IOException e) {
                Log.e(TAG, "error exporting", e);
                return false;
            } catch (final ImportExportException e) {
                Log.e(TAG, "error exporting", e);
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            mBackupAdapter.reload();
            progress.dismiss();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> list, View arg1, int position, long id) {
        final BackupItem item = mBackupAdapter.getItem(position);
        try {
            final int count = MessageUtils.importJson(this, item.path);
            Toast.makeText(this, "Successfully imported " + count + " items.", Toast.LENGTH_LONG)
                    .show();

        } catch (final IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
}
