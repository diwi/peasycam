package debug;

import java.util.Locale;

import peasycam3.PeasyCam;
import peasycam3.org.apache.commons.math.geometry.Rotation;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.opengl.PGraphicsOpenGL;


public class Peasycam_multipleCanvas extends PApplet {
  


  int view_w = 1480;
  int view_h =  820;
  
  int border = 5;
  int nx = 3;
  int ny = 2;
  PeasyCam[] cameras = new PeasyCam[nx * ny];
  
  public void settings() {
    size(view_w, view_h, P2D);
    smooth(0);
  }
  
  public void setup() {
    surface.setResizable(true);

    int dimx = (view_w / nx);
    int dimy = (view_h / ny);

    for(int y = 0; y < ny; y++){
      for(int x = 0; x < nx; x++){
        int id = y * nx + x;
        
        int cw = dimx - border * 2;
        int ch = dimy - border * 2;
        int cx = x * dimx + border;
        int cy = y * dimy + border;
        PGraphics canvas = createGraphics(cw, ch, P3D);
        canvas.smooth(8);
        
        cameras[id] = new PeasyCam(canvas, 300);
        cameras[id].setViewport(cx, cy, cw, ch);
      }
    }
    
  }
  
  
  
  
  public boolean resize(){
    if(width == view_w && height == view_h){
      return false;
    }
    
    view_w = width;
    view_h = height;
    
    int dimx = (view_w / nx);
    int dimy = (view_h / ny);

    for(int y = 0; y < ny; y++){
      for(int x = 0; x < nx; x++){
        int id = y * nx + x;
        
        int cw = dimx - border * 2;
        int ch = dimy - border * 2;
        int cx = x * dimx + border;
        int cy = y * dimy + border;
        
        // release old canvas
        PGraphicsOpenGL canvas = (PGraphicsOpenGL) cameras[id].getCanvas();
        if(canvas != null){
          canvas.dispose();
        }
        
        canvas = (PGraphicsOpenGL) createGraphics(cw, ch, P3D);
        canvas.smooth(8);
        
        // resize, while keeping the reference
//        {
//          int smooth = canvas.smooth;
//          canvas.dispose(); 
//          canvas.setPrimary(false);
//          canvas.setParent(this);
//          canvas.setSize(cw, ch);
//          canvas.initialized = false; 
//          canvas.smooth = smooth;
//        }
          
        cameras[id].setViewport(cx, cy, cw, ch);
      }
    }
    
    
    return true;
  }
  
  public void draw(){
    resize();
    
 
    for(int i = 0; i < cameras.length; i++){
      displayScene(cameras[i], i);
    }
   
    background(0);
    for(PeasyCam cam : cameras){
      float[] view = cam.getViewport();
      image(cam.getCanvas(), view[0], view[1], view[2], view[3]);
    }
  }
  
  
  public void displayScene(PeasyCam cam, int ID){

    PGraphics canvas = cam.getCanvas();
    
    int w = canvas.width;
    int h = canvas.height;
    
    if(canvas != this.g) canvas.beginDraw();
    
    canvas.perspective(90 * DEG_TO_RAD, w/(float)h, 1, 5000);
    
    // 3D scene
    canvas.ambientLight(128, 128, 128);
    canvas.pointLight(255, 128, 64, -200, -200, 10);
    canvas.pointLight(64, 128, 255, +200, +200, 10);
    
    canvas.background(32);
    
    canvas.rectMode(CENTER);
    canvas.noStroke();
    canvas.fill(128);
    canvas.rect(0, 0, 400, 400);
    
    canvas.strokeWeight(2);
    canvas.stroke(255, 64,  0); line(0,0,0,100,0,0);
    canvas.stroke( 32,255, 32); line(0,0,0,0,100,0);
    canvas.stroke(  0, 64,255); line(0,0,0,0,0,100);
    
    canvas.translate(80,80,80);
    canvas.noStroke();
    canvas.fill(128);
    canvas.box(50);
    
    canvas.translate(0,-80,0);
    canvas.noStroke();
    canvas.fill(128);
    canvas.box(60);
    
    
    // screen-aligned 2D HUD
    cam.beginHUD();


    canvas.rectMode(CORNER);
    canvas.noStroke();
    canvas.fill(0, 32); canvas.rect(   0,   0, 80, 40);
    
//    CameraState state = cam.getState();
    
    float    dist  = cam.getDistance();
    float[]  lookat = cam.getCenter();
    Rotation rot   = cam.getRotation();
    
    String txt_ID       = String.format(Locale.ENGLISH, "camera %d", ID);
    String txt_dist     = String.format(Locale.ENGLISH, "distance %5.1f", dist);
    String txt_lookat   = String.format(Locale.ENGLISH, "lookat %4.1f, %4.1f, %4.1f", lookat[0], lookat[1], lookat[2]);
    String txt_rotation = PeasyCam.Utils.getRotationString(rot);
    int tx = 10;
    int ty = 5;
    
    canvas.fill(255);
    canvas.textSize(11);
    canvas.text(txt_ID      , tx, ty += 12);
    canvas.text(txt_dist    , tx, ty += 12);
    canvas.text(txt_lookat  , tx, ty += 12);
    canvas.text(txt_rotation, tx, ty += 12);

    cam.endHUD();
    


    
    if(canvas != this.g) canvas.endDraw();
  }
  
  
  

  
  public void keyReleased(){

  }

  
  public static void main(String args[]) {
    PApplet.main(new String[] { Peasycam_multipleCanvas.class.getName() });
  }
  
}

