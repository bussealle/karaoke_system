package musicle;

import java.io.File;
import javax.swing.filechooser.FileFilter;

/* FileChooser用の.wavフィルター */
public class wavFilter extends FileFilter{

  @Override
  public boolean accept(File f){
    if (f.isDirectory()){
      return true;
    }

    String ext = getExtension(f);
    if (ext != null){
        return ext.equals("wav");
    }

    return false;
  }
  @Override
  public String getDescription(){
    return "WAVファイル";
  }
  
  private String getExtension(File f){
    String ext = null;
    String filename = f.getName();
    int dotIndex = filename.lastIndexOf('.');

    if ((dotIndex > 0) && (dotIndex < filename.length() - 1)){
      ext = filename.substring(dotIndex + 1).toLowerCase();
    }
      
    return ext;
  }
}
