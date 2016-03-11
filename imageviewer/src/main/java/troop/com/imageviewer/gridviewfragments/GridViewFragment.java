package troop.com.imageviewer.gridviewfragments;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;

import com.defcomk.jni.libraw.RawUtils;
import com.troop.filelogger.Logger;
import com.troop.freedcam.utils.StringUtils;


import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

import troop.com.imageviewer.BitmapHelper;
import troop.com.imageviewer.CacheHelper;
import troop.com.imageviewer.DngConvertingActivity;
import troop.com.imageviewer.DngConvertingFragment;
import troop.com.imageviewer.R;
import troop.com.imageviewer.ScreenSlideActivity;
import troop.com.imageviewer.gridimageviews.GridImageView;
import troop.com.imageviewer.holder.FileHolder;

/**
 * Created by troop on 11.12.2015.
 */
public class GridViewFragment extends BaseGridViewFragment
{
    private ImageAdapter mPagerAdapter;
    private ArrayList<FileHolder> files;
    private int mImageThumbSize = 0;
    private CacheHelper cacheHelper;
    final String TAG = GridViewFragment.class.getSimpleName();

    private Button deleteButton;
    private Button gobackButton;
    private Button filetypeButton;
    private Button rawToDngButton;
    final String savedInstanceString = "lastpath";
    private String savedInstanceFilePath;
    private FormatTypes formatsToShow = FormatTypes.all;
    private boolean pos0ret = false;
    private boolean PERMSISSIONGRANTED = false;
    final String NOIMAGE = "noimage_thumb";
    final String FOLDER = "folder_thumb";
    private Bitmap noimg;
    private Bitmap fold;


    public enum FormatTypes
    {
        all,
        raw,
        dng,
        jpg,
        jps,
        mp4,
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        super.onCreateView(inflater, container, savedInstanceState);
        files = new ArrayList<>();
        mImageThumbSize = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size);
        deleteButton = (Button)view.findViewById(R.id.button_deltePics);
        deleteButton.setVisibility(View.GONE);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (currentViewState) {
                    case normal:
                        setViewMode(ViewStates.selection);
                        break;
                    case selection:
                        //check if files are selceted
                        boolean hasfilesSelected = false;
                        for (FileHolder f : files) {
                            if (f.IsSelected()) {
                                hasfilesSelected = true;
                                break;
                            }

                        }
                        //if no files selected skip dialog
                        if (!hasfilesSelected)
                            break;
                        //else show dialog
                        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                        builder.setMessage(R.string.delete_files).setPositiveButton(R.string.yes, dialogClickListener)
                                .setNegativeButton(R.string.no, dialogClickListener).show();
                        setViewMode(ViewStates.normal);
                        break;
                }
            }
        });

        gobackButton = (Button)view.findViewById(R.id.button_goback);
        gobackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                if (currentViewState == ViewStates.normal) {
                    if (files != null && files.size() > 0) {
                        String topPath = files.get(0).getFile().getParentFile().getParentFile().getAbsolutePath() + "/";
                        String inter = StringUtils.GetInternalSDCARD() + StringUtils.DCIMFolder;
                        String external = StringUtils.GetExternalSDCARD() + StringUtils.DCIMFolder;

                        if ((inter.contains(topPath) && topPath.length() < inter.length() || topPath.equals(inter))
                                || (external.contains(topPath) && topPath.length() < external.length() || topPath.equals(external)))
                        {
                            if(topPath.equals(inter) || topPath.equals(external)) {
                                savedInstanceFilePath = null;
                                loadDefaultFolders();
                            }
                            else
                                getActivity().finish();
                        }
                        else {
                            loadFiles(files.get(0).getFile());
                            savedInstanceFilePath = files.get(0).getFile().getAbsolutePath();
                        }
                    }
                    else
                        loadDefaultFolders();
                }
                else if (currentViewState == ViewStates.selection)
                {
                    for (int i = 0; i< files.size(); i++)
                    {
                        FileHolder f = files.get(i);
                        f.SetSelected(false);
                    }
                    setViewMode(ViewStates.normal);
                }
            }
        });

        filetypeButton = (Button)view.findViewById(R.id.button_filetype);
        filetypeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPopup(v);
            }
        });

        rawToDngButton = (Button)view.findViewById(R.id.button_rawToDng);
        rawToDngButton.setVisibility(View.GONE);
        rawToDngButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArrayList<String> ar = new ArrayList<String>();
                for (FileHolder f : files) {
                    if (f.IsSelected() && f.getFile().getAbsolutePath().endsWith("raw")) {
                        ar.add(f.getFile().getAbsolutePath());
                    }

                }
                for (FileHolder f : files) {
                    f.SetSelected(false);
                }
                setViewMode(ViewStates.normal);
                final Intent i = new Intent(getActivity(), DngConvertingActivity.class);
                String[] t = new String[ar.size()];
                ar.toArray(t);
                i.putExtra(DngConvertingFragment.EXTRA_FILESTOCONVERT, t);
                startActivity(i);
            }
        });
        mPagerAdapter = new ImageAdapter(getContext());
        gridView.setAdapter(mPagerAdapter);

        return view;
    }

    @Override
    protected void inflate(LayoutInflater inflater, ViewGroup container) {
        view = inflater.inflate(R.layout.gridviewfragment, container, false);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState != null)
        {
            savedInstanceFilePath = (String) savedInstanceState.get(savedInstanceString);

        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(savedInstanceString, savedInstanceFilePath);
        super.onSaveInstanceState(outState);
    }

    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which){
                case DialogInterface.BUTTON_POSITIVE:
                    for (int i = 0; i< files.size(); i++)
                    {
                        if (files.get(i).IsSelected())
                        {
                            cacheHelper.deleteFileFromDiskCache(files.get(i).getFile().getName());
                            boolean d = files.get(i).getFile().delete();
                            Logger.d(TAG, "File delted:" + files.get(i).getFile().getName() + " :" + d);
                            files.remove(i);
                            i--;
                        }
                    }
                    mPagerAdapter.notifyDataSetChanged();
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    //No button clicked
                    break;
            }
        }
    };



    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        cacheHelper = new CacheHelper(getActivity());
        if(savedInstanceState != null){
            savedInstanceFilePath = (String) savedInstanceState.get(savedInstanceString);
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            checkMarshmallowPermissions();
        }
        else
            load();
    }

    private void load()
    {
        if (savedInstanceFilePath == null)
            loadDefaultFolders();
        else
            loadFiles(new File(savedInstanceFilePath));

    }

    private void checkMarshmallowPermissions() {
        if (getActivity().checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                            Manifest.permission.READ_EXTERNAL_STORAGE,

                    },
                    1);
        }
        else
            load();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
        {
            load();
        }
        else
            getActivity().finish();
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


    private void loadDefaultFolders()
    {
        File internalSDCIM = new File(StringUtils.GetInternalSDCARD() + StringUtils.DCIMFolder);
        File externalSDCIM = new File(StringUtils.GetExternalSDCARD() + StringUtils.DCIMFolder);
        ArrayList<FileHolder> list = new ArrayList<FileHolder>();
        File[] f = internalSDCIM.listFiles();
        if (f == null)
            return;
        for (int i = 0; i< f.length; i++)
        {
            if (!f[i].isHidden())
                list.add(new FileHolder(f[i]));
        }
        try {
            f = externalSDCIM.listFiles();
            for (int i = 0; i< f.length; i++)
            {
                if (!f[i].isHidden())
                    list.add(new FileHolder(f[i]));
            }
        }
        catch (Exception ex) {
            Logger.d(TAG, "No external SD!");
        }
        files = list;
        sortList(files);
        mPagerAdapter.notifyDataSetChanged();
    }

    private void loadFiles(File file)
    {
        ArrayList<FileHolder> list = new ArrayList<FileHolder>();
        File[]f = file.listFiles();
        for (int i = 0; i< f.length; i++)
        {
            if (!f[i].isHidden()) {
                if (formatsToShow == FormatTypes.all && (
                        f[i].getAbsolutePath().endsWith("jpg")
                        || f[i].getAbsolutePath().endsWith("jps")
                        ||f[i].getAbsolutePath().endsWith("raw")
                        ||f[i].getAbsolutePath().endsWith("dng")
                                ||   f[i].getAbsolutePath().endsWith("mp4")
                ))
                    list.add(new FileHolder(f[i]));
                else if(formatsToShow == FormatTypes.dng && f[i].getAbsolutePath().endsWith("dng"))
                    list.add(new FileHolder(f[i]));
                else if(formatsToShow == FormatTypes.raw && f[i].getAbsolutePath().endsWith("raw"))
                    list.add(new FileHolder(f[i]));
                else if(formatsToShow == FormatTypes.jps && f[i].getAbsolutePath().endsWith("jps"))
                    list.add(new FileHolder(f[i]));
                else if(formatsToShow == FormatTypes.jpg && f[i].getAbsolutePath().endsWith("jpg"))
                    list.add(new FileHolder(f[i]));
                else if(formatsToShow == FormatTypes.mp4 && f[i].getAbsolutePath().endsWith("mp4"))
                    list.add(new FileHolder(f[i]));
            }
        }
        files = list;
        sortList(files);
        mPagerAdapter.notifyDataSetChanged();
    }

    private void sortList(ArrayList<FileHolder> filesar)
    {
        Collections.sort(filesar, new Comparator<FileHolder>() {
            public int compare(FileHolder f1, FileHolder f2) {
                return Long.valueOf(f2.getFile().lastModified()).compareTo(f1.getFile().lastModified());
            }
        });
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id)
    {
        switch (currentViewState)
        {
            case normal:
                if (!files.get(position).IsFolder()) {
                    final Intent i = new Intent(getActivity(), ScreenSlideActivity.class);
                    i.putExtra(ScreenSlideActivity.EXTRA_IMAGE, position);
                    if (files != null &&files.size() >0)
                    {
                        if (!files.get(position).IsFolder())
                            i.putExtra(ScreenSlideActivity.IMAGE_PATH, files.get(position).getFile().getParentFile().getAbsolutePath());
                        else
                            i.putExtra(ScreenSlideActivity.IMAGE_PATH, files.get(position).getFile().getAbsolutePath());
                    }
                    i.putExtra(ScreenSlideActivity.FileType, formatsToShow.name());
                    startActivity(i);
                }
                else
                {
                    savedInstanceFilePath = files.get(position).getFile().getAbsolutePath();
                    loadFiles(files.get(position).getFile());

                }
                break;
            case selection:
                if (files.get(position).IsSelected())
                    files.get(position).SetSelected(false);
                else
                    files.get(position).SetSelected(true);
        }
    }


    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id)
    {
        switch (currentViewState)
        {
            case normal:
                setViewMode(ViewStates.selection);
                files.get(position).SetSelected(true);

        }
        return false;
    }



    private class ImageAdapter extends BaseAdapter
    {
        private final Context mContext;

        public ImageAdapter(Context context) {
            super();
            mContext = context;
            pos0ret = false;
        }

        @Override
        public int getCount() {
            return files.size();
        }

        @Override
        public Object getItem(int position) {
            return files.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }


        @Override
        public View getView(int position, View convertView, ViewGroup container) {
            GridImageView imageView;
            if (convertView == null) { // if it's not recycled, initialize some attributes
                imageView = new GridImageView(mContext);
            } else {
                imageView = (GridImageView) convertView;
            }
            if (imageView.getFileHolder() == null || !imageView.getFileHolder().equals(files.get(position))) {
                imageView.SetEventListner(files.get(position));
                Logger.d(TAG, "pos:" + position + " imageviewState: " + files.get(position).GetCurrentViewState() + " /GridState:" + currentViewState + " filename:" + files.get(position).getFile().getName() +
                        " ischecked:" + files.get(position).IsSelected());
                loadBitmap(files.get(position).getFile(), imageView); // Load image into ImageView
            }
            return imageView;
        }
    }

    private static BitmapWorkerTask getBitmapWorkerTask(GridImageView imageView) {
        if (imageView != null) {
            final Drawable drawable = imageView.getDrawable();
            if (drawable instanceof AsyncDrawable) {
                final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
                return asyncDrawable.getBitmapWorkerTask();
            }
        }
        return null;
    }

    public void loadBitmap(File file, GridImageView imageView)
    {
        if (cancelPotentialWork(file, imageView))
        {
            final BitmapWorkerTask task = new BitmapWorkerTask(imageView);
            if (!file.isDirectory())
            {
                if (noimg == null)
                    noimg = cacheHelper.getBitmapFromMemCache(NOIMAGE);
                if (noimg == null)
                    noimg = cacheHelper.getBitmapFromDiskCache(NOIMAGE);
                if (noimg == null)
                {
                    noimg = BitmapFactory.decodeResource(getResources(), R.drawable.noimage);
                    cacheHelper.addBitmapToCache(NOIMAGE, noimg);
                }

                final AsyncDrawable asyncDrawable =
                        new AsyncDrawable(getResources(), noimg, task);
                imageView.setImageDrawable(asyncDrawable);
            }
            else
            {
                if (fold == null)
                    fold = cacheHelper.getBitmapFromMemCache(FOLDER);
                if (fold == null)
                    fold = cacheHelper.getBitmapFromDiskCache(FOLDER);
                if (fold == null)
                {
                    fold = BitmapFactory.decodeResource(getResources(), R.drawable.folder);
                    cacheHelper.addBitmapToCache(FOLDER, fold);

                }
                final AsyncDrawable asyncDrawable =
                        new AsyncDrawable(getResources(),fold, task);
                imageView.setImageDrawable(asyncDrawable);
            }
            String f = file.getName();
            if (!file.isDirectory()) {
                imageView.SetFolderName("");
                imageView.SetFileEnding(f.substring(f.length() - 3));
            }

            else {
                imageView.SetFileEnding("");
                imageView.SetFolderName(f);
            }

            task.execute(file);
        }
    }


    static class AsyncDrawable extends BitmapDrawable {
        private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

        public AsyncDrawable(Resources res, Bitmap bitmap,
                             BitmapWorkerTask bitmapWorkerTask) {
            super(res, bitmap);
            bitmapWorkerTaskReference =
                    new WeakReference<BitmapWorkerTask>(bitmapWorkerTask);
        }

        public BitmapWorkerTask getBitmapWorkerTask() {
            return bitmapWorkerTaskReference.get();
        }
    }

    public static boolean cancelPotentialWork(File file, GridImageView imageView) {
        final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

        if (bitmapWorkerTask != null) {
            final File bitmapData = bitmapWorkerTask.file;
            if (bitmapData != file) {
                // Cancel previous task
                bitmapWorkerTask.cancel(true);
            } else {
                // The same work is already in progress
                return false;
            }
        }
        // No task associated with the ImageView, or an existing task was cancelled
        return true;
    }

    class BitmapWorkerTask extends AsyncTask<File, Void, Bitmap> {
        private final WeakReference<GridImageView> imageViewReference;
        private File file;

        public BitmapWorkerTask(GridImageView imageView) {
            // Use a WeakReference to ensure the ImageView can be garbage collected
            imageViewReference = new WeakReference<GridImageView>(imageView);
        }

        // Decode image in background.
        @Override
        protected Bitmap doInBackground(File... params) {
            file = params[0];
            return getBitmap(file);
        }

        // Once complete, see if ImageView is still around and set bitmap.
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (imageViewReference != null && bitmap != null) {
                final GridImageView imageView = imageViewReference.get();
                if (imageView != null)
                {
                    imageView.setImageBitmap(bitmap);
                }
            }
        }
    }

    private Bitmap getBitmap(File file)
    {
        return BitmapHelper.getBitmap(file,true, cacheHelper,mImageThumbSize,mImageThumbSize);
    }

    private void setViewMode(ViewStates viewState)
    {
        this.currentViewState = viewState;
        for (int i = 0; i< files.size(); i++)
        {
            FileHolder f = files.get(i);
            f.SetViewState(viewState);

        }
        mPagerAdapter.notifyDataSetChanged();
        switch (viewState)
        {
            case normal:
            {
                deleteButton.setVisibility(View.GONE);
                rawToDngButton.setVisibility(View.GONE);
                break;
            }
            case selection:
                deleteButton.setVisibility(View.VISIBLE);
                rawToDngButton.setVisibility(View.VISIBLE);
                break;

        }
    }

    public void showPopup(View v) {
        PopupMenu popup = new PopupMenu(this.getContext(), v);
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int i = item.getItemId();
                if (i == R.id.all)
                {
                    filetypeButton.setText(R.string.ALL);
                    formatsToShow = FormatTypes.all;
                }
                else if (i == R.id.raw)
                {
                    filetypeButton.setText("RAW");
                    formatsToShow = FormatTypes.raw;
                }
                else if (i == R.id.dng)
                {
                    filetypeButton.setText("DNG");
                    formatsToShow = FormatTypes.dng;
                }
                else if (i == R.id.jps)
                {
                    filetypeButton.setText("JPS");
                    formatsToShow = FormatTypes.jps;
                }
                else if (i == R.id.jpg)
                {
                    filetypeButton.setText("JPG");
                    formatsToShow = FormatTypes.jpg;
                }
                else if (i == R.id.mp4)
                {
                    filetypeButton.setText("MP4");
                    formatsToShow = FormatTypes.mp4;
                }
                if (savedInstanceFilePath != null)
                    loadFiles(new File(savedInstanceFilePath));
                return false;

            }
        });
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.filetypepopupmenu, popup.getMenu());
        popup.show();
    }

}
