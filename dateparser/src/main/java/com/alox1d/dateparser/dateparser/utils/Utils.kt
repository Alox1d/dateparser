package com.alox1d.dateparser.dateparser.utils

import android.content.Context
import android.os.Environment
import java.io.*
import java.lang.Exception
import java.lang.StringBuilder
import java.nio.channels.FileChannel

class Utils {
    //    public static void showTips(String toastText) {
    //        runOnUiThread {
    //            Toast.makeText(applicationContext, toastText, Toast.LENGTH_LONG).show()
    //
    //        }
    //    }
    //    public static String getDate(long date) {
    //        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yy");
    //        return dateFormat.format(date);
    //
    //    }
    //
    //    public static String getTime(long time) {
    //        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
    //        return timeFormat.format(time);
    //    }
    //
    //    public static String getFullDate(long date) {
    //        SimpleDateFormat fullDateFormat = new SimpleDateFormat("dd.MM.yy HH:mm");
    //        return fullDateFormat.format(date);
    //    }
    //
    //    public static String getFullDateWithSecs(long date) {
    //        SimpleDateFormat fullDateFormat = new SimpleDateFormat("dd.MM.yy HH:mm:ss");
    //        return fullDateFormat.format(date);
    //    }
    //
    //    public static String colorHex(int color) {
    //        int r = Color.red(color);
    //        int g = Color.green(color);
    //        int b = Color.blue(color);
    //        return String.format(Locale.getDefault(), "#%02X%02X%02X", r, g, b);
    //    }
    //
    //    public static Map<Integer, String> getHeadingTypeface() {
    //        Map<Integer, String> typefaceMap = new HashMap<>();
    //        typefaceMap.put(Typeface.NORMAL, "fonts/GreycliffCF-Bold.ttf");
    //        typefaceMap.put(Typeface.BOLD, "fonts/GreycliffCF-Heavy.ttf");
    //        typefaceMap.put(Typeface.ITALIC, "fonts/GreycliffCF-Heavy.ttf");
    //        typefaceMap.put(Typeface.BOLD_ITALIC, "fonts/GreycliffCF-Bold.ttf");
    //        return typefaceMap;
    //    }
    //
    //    public static Map<Integer, String> getContentface() {
    //        Map<Integer, String> typefaceMap = new HashMap<>();
    //        typefaceMap.put(Typeface.NORMAL, "fonts/Lato-Medium.ttf");
    //        typefaceMap.put(Typeface.BOLD, "fonts/Lato-Bold.ttf");
    //        typefaceMap.put(Typeface.ITALIC, "fonts/Lato-MediumItalic.ttf");
    //        typefaceMap.put(Typeface.BOLD_ITALIC, "fonts/Lato-BoldItalic.ttf");
    //        return typefaceMap;
    //    }
    //
    //
    //
    //    @ColorInt
    //    public static int getThemeColor
    //            (
    //                    @NonNull final Context context,
    //                    @AttrRes final int attributeColor
    //            ) {
    //        final TypedValue value = new TypedValue();
    //        context.getTheme().resolveAttribute(attributeColor, value, true);
    //        return value.data;
    //    }
    ////    public static String getUnicodeString(String myString) {
    ////        String text = "";
    ////        try {
    ////
    ////            byte[] utf8Bytes = myString.getBytes("UTF8");
    ////            text = new String(utf8Bytes, "UTF8");
    ////
    ////        } catch (UnsupportedEncodingException e) {
    ////            e.printStackTrace();
    ////        }
    ////        return text;
    ////    }
    ////    public static String escapeUnicodeText(String input) {
    ////
    ////        StringBuilder b = new StringBuilder(input.length());
    ////
    ////        java.util.Formatter f = new java.util.Formatter(b);
    ////
    ////        for (char c : input.toCharArray()) {
    ////            if (c < 128) {
    ////                b.append(c);
    ////            } else {
    ////                f.format("\\u%04x", (int) c);
    ////            }
    ////        }
    ////
    ////        return b.toString();
    ////    }
    //
    //
    //    public static class ViewGroupUtils {
    //
    //        public static ViewGroup getParent(View view) {
    //            return (ViewGroup) view.getParent();
    //        }
    //
    //        public static void removeView(View view) {
    //            ViewGroup parent = getParent(view);
    //            if (parent != null) {
    //                parent.removeView(view);
    //            }
    //        }
    //
    //        public static void replaceView(View currentView, View newView) {
    //            ViewGroup parent = getParent(currentView);
    //            if (parent == null) {
    //                return;
    //            }
    //            final int index = parent.indexOfChild(currentView);
    //            removeView(currentView);
    //            removeView(newView);
    //            parent.addView(newView, index);
    //        }
    //    }
    object FileUtils {
        fun readRawTextFile(ctx: Context, resId: Int): String? {
            val inputStream = ctx.resources.openRawResource(resId)
            val inputreader = InputStreamReader(inputStream)
            val buffreader = BufferedReader(inputreader)
            var line: String?
            val text = StringBuilder()
            try {
                while (buffreader.readLine().also { line = it } != null) {
                    text.append(line)
                    text.append('\n')
                }
            } catch (e: IOException) {
                return null
            }
            text.delete(text.length - 1, text.length)
            return text.toString()
        }

        @Throws(IOException::class)
        fun saveToFile(context: Context, fileName: String?, `object`: Any?): File {
            val fileObject = File(context.getExternalFilesDir(null), fileName)
            // file does not exist, create it
            if (!fileObject.exists()) fileObject.createNewFile()
            val fos = FileOutputStream(fileObject)
            val os = ObjectOutputStream(fos)
            os.writeObject(`object`)
            os.close()
            fos.close()
            return fileObject
        }

        @Throws(IOException::class, ClassNotFoundException::class)
        fun <T> loadFromFile(file: File?): T {
            val fis = FileInputStream(file)
            val `is` = ObjectInputStream(fis)
            val simpleClass = `is`.readObject() as T
            `is`.close()
            fis.close()
            return simpleClass
        }

        /**
         * Creates the specified `toFile` as a byte for byte copy of the
         * `fromFile`. If `toFile` already exists, then it
         * will be replaced with a copy of `fromFile`. The name and path
         * of `toFile` will be that of `toFile`.<br></br>
         * <br></br>
         * * Note: `fromFile` and `toFile` will be closed by
         * this function.*
         *
         * @param fromFile - FileInputStream for the file to copy from.
         * @param toFile   - FileInputStream for the file to copy to.
         */
        @Throws(IOException::class)
        fun copyFile(fromFile: FileInputStream, toFile: FileOutputStream) {
            var fromChannel: FileChannel? = null
            var toChannel: FileChannel? = null
            try {
                fromChannel = fromFile.channel
                toChannel = toFile.channel
                fromChannel.transferTo(0, fromChannel.size(), toChannel)
            } finally {
                try {
                    fromChannel?.close()
                } finally {
                    toChannel?.close()
                }
            }
        }

        @Throws(IOException::class)
        fun copyFile(from: File?, to: File) {
            //    String dir=Environment.getExternalStorageDirectory().getAbsolutePath();
//    File sd = new File(dir);
            val data = Environment.getDataDirectory()
            var source: FileChannel? = null
            var destination: FileChannel? = null
            val backupDBPath = to.absolutePath
            //    File currentDB = new File(sd, currentDBPath);
            val backupDB = File(data, backupDBPath)
            source = FileInputStream(from).channel
            destination = FileOutputStream(to).channel
            destination.transferFrom(source, 0, source.size())
            source.close()
            destination.close()
            //        Toast.makeText(this, "Your Database is Imported !!", Toast.LENGTH_SHORT).show();
        }

        //        public static void copyFile(InputStream from, File to) throws IOException {
        //            //    String dir=Environment.getExternalStorageDirectory().getAbsolutePath();
        ////    File sd = new File(dir);
        //            File data = Environment.getDataDirectory();
        //            FileChannel source = null;
        //            FileChannel destination = null;
        //            String backupDBPath = to.getAbsolutePath();
        ////    File currentDB = new File(sd, currentDBPath);
        //            File backupDB = new File(data, backupDBPath);
        //
        //            source = new FileInputStream(from.tra).getChannel();
        //            destination = new FileOutputStream(to).getChannel();
        //            destination.transferFrom(source, 0, source.size());
        //            source.close();
        //            destination.close();
        ////        Toast.makeText(this, "Your Database is Imported !!", Toast.LENGTH_SHORT).show();
        //        }
        fun copyInputStreamToFile(`in`: InputStream, file: File?) {
            try {
                val out: OutputStream = FileOutputStream(file)
                val buf = ByteArray(1024)
                var len: Int
                while (`in`.read(buf).also { len = it } > 0) {
                    out.write(buf, 0, len)
                }
                out.close()
                `in`.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        /** Creates parent directories if necessary. Then returns file  */
        fun fileWithDirectoryAssurance(directory: String, filename: String): File {
            val dir = File(directory)
            if (!dir.exists()) dir.mkdirs()
            return File("$directory/$filename")
        }
    } //
    //    public static class ImageUtils {
    //        public static Uri getImageUri(Context inContext, Bitmap inImage) {
    //            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    //            inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
    //            String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
    //            return Uri.parse(path);
    //        }
    //
    //        public static String getRealPathFromURI(Uri uri, Context context) {
    //            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
    //            cursor.moveToFirst();
    //            int idx = cursor.getColumnIndex(MediaStore.Images.Media.DATA); // MediaStore.Images.ImageColumns.DATA
    //            return cursor.getString(idx);
    //        }
    //
    //        public static void deleteImg(Context context, String imgPath) {
    //            // delete file
    ////                                        boolean isNotEmptyFound = true;
    ////                                        File fdelete = new File(imgPath);
    ////                                        if (fdelete.exists()) {
    ////                                            if (fdelete.delete()) {
    ////                                                Toast.makeText(getContext(),"file Deleted :" + imgPath, Toast.LENGTH_SHORT);
    ////                                            } else {
    ////                                                Toast.makeText(getContext(),"file not Deleted :" + imgPath, Toast.LENGTH_SHORT);
    ////
    ////                                            }
    ////                                        }
    //
    //
    //            // Set up the projection (we only need the ID)
    //            String[] projection = {MediaStore.Images.Media._ID};
    //
    //            // Match on the file path
    //            String selection = MediaStore.Images.Media.DATA + " = ?";
    //            String[] selectionArgs = new String[]{imgPath};
    //
    //            // Query for the ID of the media matching the file path
    //            Uri queryUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
    //            ContentResolver contentResolver = context.getContentResolver();
    //            Cursor c = contentResolver.query(queryUri, projection, selection, selectionArgs, null);
    //            if (c.moveToFirst()) {
    //                // We found the ID. Deleting the item via the content provider will also remove the file
    //                long id = c.getLong(c.getColumnIndexOrThrow(MediaStore.Images.Media._ID));
    //                Uri deleteUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
    //                contentResolver.delete(deleteUri, null, null);
    //            } else {
    //                // File not found in media store DB
    //            }
    //            c.close();
    //        }
    //
    //        public static Uri saveImage(Bitmap bitmap, Context context, String folderName) throws FileNotFoundException {
    //            if (android.os.Build.VERSION.SDK_INT >= 29) {
    //                ContentValues values = getContentValues();
    //                values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/" + folderName);
    //                values.put(MediaStore.Images.Media.IS_PENDING, true);
    //                // RELATIVE_PATH and IS_PENDING are introduced in API 29.
    //
    //                Uri uri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    //                if (uri != null) {
    //                    saveImageToStream(bitmap, context.getContentResolver().openOutputStream(uri));
    //                    values.put(MediaStore.Images.Media.IS_PENDING, false);
    //                    context.getContentResolver().update(uri, values, null, null);
    //                    return uri;
    //                }
    //            } else {
    //                File directory = new File(Environment.getExternalStorageDirectory().toString() + separator + folderName);
    //                // getExternalStorageDirectory is deprecated in API 29
    //
    //                if (!directory.exists()) {
    //                    directory.mkdirs();
    //                }
    //                String fileName = Utils.getFullDateWithSecs(System.currentTimeMillis()) + ".jpeg";
    //                File file = new File(directory, fileName);
    //                saveImageToStream(bitmap, new FileOutputStream(file));
    //                if (file.getAbsolutePath() != null) {
    //                    ContentValues values = getContentValues();
    //                    values.put(MediaStore.Images.Media.DATA, file.getAbsolutePath());
    //                    // .DATA is deprecated in API 29
    //                    return context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    //                }
    //            }
    //            return null;
    //        }
    //
    //        private static ContentValues getContentValues() {
    //            ContentValues values = new ContentValues();
    //            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
    //            values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000);
    //            values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
    //            return values;
    //        }
    //
    //        private static void saveImageToStream(Bitmap bitmap, OutputStream outputStream) {
    //            if (outputStream != null) {
    //                try {
    //                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
    //                    outputStream.close();
    //                } catch (Exception e) {
    //                    e.printStackTrace();
    //                }
    //            }
    //        }
    //
    //    }
    //
    //    public static final class EscapedWriter
    //            extends Writer {
    //
    //        private static final char[] hex = {
    //                '0', '1', '2', '3',
    //                '4', '5', '6', '7',
    //                '8', '9', 'a', 'b',
    //                'c', 'd', 'e', 'f'
    //        };
    //
    //        private final Writer writer;
    //
    //        // I/O components are usually implemented in not thread-safe manner
    //        // so we can save some time on constructing a single UTF-16 escape
    //        private final char[] escape = {'\\', 'u', 0, 0, 0, 0};
    //
    //        public EscapedWriter(final Writer writer) {
    //            this.writer = writer;
    //        }
    //
    //        // This implementation is not very efficient and is open for enhancements:
    //        // * constructing a single "normalized" buffer character array so that it could be passed to the downstream writer
    //        //   rather than writing characters one by one
    //        // * etc...
    //        @Override
    //        public void write(final char[] buffer, final int offset, final int length)
    //                throws IOException {
    //            for (int i = offset; i < length; i++) {
    //                final int ch = buffer[i];
    //                if (ch < 128) {
    //                    writer.write(ch);
    //                } else {
    //                    escape[2] = hex[(ch & 0xF000) >> 12];
    //                    escape[3] = hex[(ch & 0x0F00) >> 8];
    //                    escape[4] = hex[(ch & 0x00F0) >> 4];
    //                    escape[5] = hex[ch & 0x000F];
    //                    writer.write(escape);
    //                }
    //            }
    //        }
    //
    //        @Override
    //        public void flush()
    //                throws IOException {
    //            writer.flush();
    //        }
    //
    //        @Override
    //        public void close()
    //                throws IOException {
    //            writer.close();
    //        }
    //
    //        // Some java.io.Writer subclasses may use java.lang.Object.toString() to materialize their accumulated state by design
    //        // so it has to be overridden and forwarded as well
    //        @Override
    //        public String toString() {
    //            return writer.toString();
    //        }
    //
    //    }
}