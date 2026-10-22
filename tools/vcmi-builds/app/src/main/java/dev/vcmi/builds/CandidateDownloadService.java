package dev.vcmi.builds;

import android.app.*;
import android.content.*;
import android.os.*;
import org.json.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.util.concurrent.*;

/** One foreground transfer owns staging files; Activity instances only observe persisted status. */
public final class CandidateDownloadService extends Service {
    static volatile boolean running;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private volatile HttpURLConnection connection;
    private volatile boolean cancelled;
    private boolean started;
    private static final String CHANNEL="candidate-downloads";
    static SharedPreferences state(Context context) { return context.getSharedPreferences("download-state",MODE_PRIVATE); }
    private void status(String message,boolean busy,String key) {
        state(this).edit().putString("message",message).putBoolean("busy",busy).putString("key",key).apply();
    }
    private Notification notification(String message) {
        Intent cancel=new Intent(this,CandidateDownloadService.class).setAction("cancel");
        PendingIntent stop=PendingIntent.getService(this,1,cancel,PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent open=PendingIntent.getActivity(this,2,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        return new Notification.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("VCMI candidate").setContentText(message).setContentIntent(open).setOngoing(true)
            .addAction(new Notification.Action.Builder(null,"Cancel",stop).build()).build();
    }
    @Override public void onCreate() {
        super.onCreate(); running=true;
        File[] leftovers=getCacheDir().listFiles();
        if (leftovers!=null) for (File file:leftovers)
            if (file.getName().matches("candidate-(download|apk)-[a-f0-9-]+\\.part")) file.delete();
        getSystemService(NotificationManager.class).createNotificationChannel(new NotificationChannel(CHANNEL,"Build downloads",NotificationManager.IMPORTANCE_LOW));
    }
    @Override public int onStartCommand(Intent intent,int flags,int startId) {
        if (intent!=null && "cancel".equals(intent.getAction())) { cancel(); return START_NOT_STICKY; }
        if (started) return START_NOT_STICKY;
        started=true; startForeground(7,notification("Starting download"));
        if (intent==null) { status("Download interrupted — retry from Builds.",false,""); stopSelf(); return START_NOT_STICKY; }
        final String payload=intent.getStringExtra("candidate");
        status("Downloading candidate…",true,"");
        worker.execute(()->download(payload)); return START_NOT_STICKY;
    }
    private void checkCancelled() throws IOException {
        if (cancelled || Thread.currentThread().isInterrupted()) throw new IOException("Download cancelled");
    }
    private void download(String payload) {
        String stagingId=java.util.UUID.randomUUID().toString();
        File archive=new File(getCacheDir(),"candidate-download-"+stagingId+".part"), staged=new File(getCacheDir(),"candidate-apk-"+stagingId+".part");
        try {
            checkCancelled();
            JSONObject selected=new JSONObject(payload);
            String repo=GitHubClient.repository(selected.getString("repo"));
            String artifact=selected.getString("artifact_id");
            ValidationPlan.require(artifact.matches("[0-9]+"),"Invalid artifact identity");
            connection=GitHubClient.connect("https://api.github.com/repos/"+repo+"/actions/artifacts/"+artifact+"/zip",new TokenStore(this).load(),"application/vnd.github+json");
            long expected=connection.getContentLengthLong(), total=0,last=0;
            try (InputStream in=connection.getInputStream(); OutputStream out=new FileOutputStream(archive)) {
                byte[] bytes=new byte[65536]; int n;
                while ((n=in.read(bytes))!=-1) {
                    checkCancelled(); total+=n;
                    ValidationPlan.require(total<=CandidateBundle.MAX_APK,"Artifact exceeds 1 GB limit"); out.write(bytes,0,n);
                    if (System.currentTimeMillis()-last>750) {
                        last=System.currentTimeMillis(); String message="Downloading: "+(total/1048576)+" MB"+(expected>0?" / "+(expected/1048576)+" MB":"");
                        status(message,true,""); getSystemService(NotificationManager.class).notify(7,notification(message));
                    }
                }
                ValidationPlan.require(expected<0 || total==expected,"Incomplete download — retry");
            } finally { connection.disconnect(); connection=null; }
            checkCancelled(); status("Verifying APK, receipt and checklist…",true,"");
            JSONObject verified=CandidateBundle.extract(archive,staged,selected);
            checkCancelled();
            CandidateBundle.verifyPackage(this,staged);
            CandidateStore store=new CandidateStore(this); String key=CandidateStore.key(verified);
            File target=store.apk(key);
            checkCancelled();
            java.nio.file.Files.move(staged.toPath(),target.toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING,java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            checkCancelled();
            CandidateStore.write(new File(store.directory(key),"candidate.json"),verified);
            status("Download verified. Ready to install.",false,key);
        } catch (Exception e) { status(cancelled?"Download cancelled.":"Download failed: "+e.getMessage(),false,""); }
        finally {
            if (connection!=null) { connection.disconnect(); connection=null; }
            archive.delete(); staged.delete(); running=false;
            new Handler(getMainLooper()).post(()->{ stopForeground(STOP_FOREGROUND_REMOVE); stopSelf(); });
        }
    }
    private void cancel() {
        cancelled=true; if (connection!=null) connection.disconnect();
        // Let even a queued transfer enter its finally block and stop the service.
        status("Cancelling download…",started,"");
        if (!started) { running=false; stopSelf(); }
    }
    @Override public void onTimeout(int startId,int fgsType) { cancel(); stopForeground(STOP_FOREGROUND_REMOVE); stopSelf(); }
    @Override public void onDestroy() { cancelled=true; worker.shutdownNow(); if (connection!=null) connection.disconnect(); running=false; super.onDestroy(); }
    @Override public IBinder onBind(Intent intent) { return null; }
}
