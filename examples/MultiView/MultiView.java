/**
 * 
 * The PeasyCam3 Processing library.
 *
 *   Copyright 2008 Jonathan Feinberg
 *   Copyright 2018 Thomas Diewald
 *
 *   https://github.com/diwi/peasycam/tree/PeasyCam3
 *
 *   Apache License: http://www.apache.org/licenses/LICENSE-2.0
 * 
 * 
 * explanatory notes:
 * 
 * This library is a derivative of the original PeasyCam Library by Jonathan Feinberg 
 * and combines new useful features with the great look and feel of the original version.
 * 
 * It is designed to be in sync with "p5.EasyCam", the Javascript version for p5js.
 * 
 * 
 */



package MultiView;

import java.util.Locale;


import peasycam3.PeasyCam;
import peasycam3.org.apache.commons.math.geometry.Rotation;
import peasycam3.org.apache.commons.math.geometry.RotationOrder;
import processing.core.PApplet;
import processing.core.PShape;
import processing.core.PVector;
import processing.opengl.PGL;
import processing.opengl.PGraphics3D;
import processing.opengl.PJOGL;


public class MultiView extends PApplet {
  
  
  //
  //
  // MultiView (advanced version)
  //
  // N x N Camera Views of the same scene, on only one PGraphics.
  //
  // In this demo only one render-target is used -> the primary PGraphics3D ... this.g
  // Each Camera still has its own mouse-handler. 
  // Only the viewport-position/dimension is used to build the camera state.
  //
  // For rendering, some OpenGL instructions (viewport, scissors) are used to
  // render the scene to its actual camera viewport position/size.
  // 
  // Window is resizeAble
  // 
  // pressed SPACE + Mouse-drag, to apply state of active camera to all others
  //
  //

  
  final int NX = 3;
  final int NY = 2;
  PeasyCam[] cameras = new PeasyCam[NX * NY];
  
  int window_w, window_h;

  PShape scene;
  
  public void settings() {
    size(1280, 720, P3D); // 3D
    smooth(8);
  }
  
  public void setup() {
    surface.setResizable(true);
    
    randomSeed(2);

    window_w = window_h = 0; // to trigger resizeScene()
 
    for(int i = 0; i < cameras.length; i++){
      cameras[i] = new PeasyCam(this, 500);
      cameras[i].setCanvas(null);      // no canvas needed
      cameras[i].setAutoUpdate(false); // update is handled manually
      
      // set some random states
      cameras[i].setRotation(new Rotation(RotationOrder.XYZ, random(-PI,PI)/8, random(-PI,PI)/4, random(-PI,PI)/1), 2000);
      cameras[i].setDistance(random(200, 600), 2000);
//      cameras[i].setCenter(random(-100, 100), random(-100, 100), random(-100, 100), 2000);
    }

    
    // create scene
    scene = createShape(GROUP);
    
    float trange = 100;
    
    for(int i = 0; i < 100; i++){
      float dx = random(10, 50);
      
      float tx = random(-trange, trange);
      float ty = random(-trange, trange);
      float tz = random(-trange, trange);
      
      float cr = random(0, 255);
      float cg = random(0, 255);
      float cb = 255-cr-cg;
      
      PShape shape = createShape(BOX, dx, dx, dx);
      shape.translate(tx,  ty, tz);
      shape.setFill(true);
      shape.setFill(color(cr,cg,cb));
      shape.setStroke(true);
      shape.setStroke(color(0));
      shape.setStrokeWeight(1.0f);

      scene.addChild(shape);
    }
 
//    frameRate(1000);
  }
  
  
  
  public void setCameraViewports(){
    int gap = 3;
    
    // tiling size
    int tilex = floor((width  - gap) / NX);
    int tiley = floor((height - gap) / NY);
   
    // viewport offset ... corrected gap due to floor()
    int offx = (width  - (tilex * NX - gap)) / 2;
    int offy = (height - (tiley * NY - gap)) / 2;
    
    // viewport dimension
    int cw = tilex - gap;
    int ch = tiley - gap;
    
    // create new viewport for each camera
    for(int y = 0; y < NY; y++){
      for(int x = 0; x < NX; x++){
        int id = y * NX + x;
        int cx = offx + x * tilex;
        int cy = offy + y * tiley;
        cameras[id].setViewport(cx, cy, cw, ch); // this is the key of this whole demo
      }
    }
   
  }
  
  
  
  public boolean resizeScene(){
    if(window_w == width && window_h == height){
      return false;
    }
    
    window_w = width;
    window_h = height;
    
    setCameraViewports();
    
    return true;
  }

  
  public void handleSuperController(){
  
    if(keyPressed && key == ' '){
      
      long delay = 150; 
      PeasyCam active  = null;
      PeasyCam focused = null;
      
      // find active or focused camera which controls the others
      for(PeasyCam cam : cameras){
        if(cam.DRAG_ACTIVE){
          active = cam;
          break;
        }
        if(cam.insideViewport(mouseX, mouseY)){
          focused = cam;
        }
      }
      
      // no active camera, try focused
      if(active == null) active = focused;
      
      // apply state to all other cameras
      if(active != null) {
        PeasyCam.State state = active.getState();
        for(PeasyCam cam : cameras){
          if(cam != active){
            cam.setState(state, delay);
          }
        }
      }
    }
    
  }
  
  
  
  public void draw(){
    
    // handle resize
    resizeScene();
    
    // update current camera states
    for(PeasyCam cam : cameras){
      cam.update();
    }

    // check if god-mode is on
    handleSuperController();
    
    // clear background once, for the whole window
    setGLGraphicsViewport(0, 0, width, height);
    background(0);
    
    // render scene once per camera/viewport
    for(int i = 0; i < cameras.length; i++){
      PeasyCam cam = cameras[i];
      
      // note: enabling/disabling styles seems a big buggy (3.3.6)
      scene.disableStyle(); // saves styles internally
      scene.enableStyle();
      
      pushMatrix();
      pushStyle();

      displayScene(cam, i);
      
      popStyle();
      popMatrix();
      
      scene.enableStyle(); // make sure we restore the initial style
    }
    
    // setGLGraphicsViewport(0, 0, width, height);
    
    String txt_fps = String.format(Locale.ENGLISH, "|  fps: %7.2f", frameRate);
    surface.setTitle("MultiView  "+txt_fps);
  }
  
  
  // some OpenGL instructions to set our custom viewport
  //   https://www.khronos.org/registry/OpenGL-Refpages/gl4/html/glViewport.xhtml
  //   https://www.khronos.org/registry/OpenGL-Refpages/gl4/html/glScissor.xhtml
  void setGLGraphicsViewport(int x, int y, int w, int h){
    PGraphics3D pg = (PGraphics3D) this.g;
    PJOGL pgl = (PJOGL) pg.beginPGL();  pg.endPGL();
    
    pgl.enable(PGL.SCISSOR_TEST);
    pgl.scissor (x,y,w,h);
    pgl.viewport(x,y,w,h);
  }

  
  public void displayScene(PeasyCam cam, int ID){
    
    int[] viewport = cam.getViewport();
    int w = viewport[2];
    int h = viewport[3];
    int x = viewport[0];
    int y = viewport[1];
    int y_inv =  height - y - h; // inverted y-axis

    // scissors-test and viewport transformation
    setGLGraphicsViewport(x, y_inv, w, h);
    
    // modelview - using camera state
    cam.apply(this.g);
   
    // projection - using camera viewport
    perspective(60 * PI/180, w/(float)h, 1, 5000);

    // clear background (scissors makes sure we only clear the region we own)
    background(16);
    
    // lights setup
    {
      noLights();
      ambientLight(96, 96, 96);
      pointLight(200,100,0, 300,300,0);
      PVector dir = new PVector(-1, +2, -3); dir.normalize();
      directionalLight(200,200,200, dir.x, dir.y, dir.z);
    }

    //scene style
    switch(ID){
    case 0: // wire
      noLights();
      scene.setStroke(true);
      scene.setStroke(color(255));
      scene.setFill(false);
      break;
    case 1: // wire
      noLights();
      scene.setStroke(true);
      scene.setStroke(color(0));
      scene.setFill(true);
      scene.setFill(color(255));
      break;
    case 2: // mono orange
      scene.setStroke(true);
      scene.setStroke(color(0));
      scene.setFill(true);
      scene.setFill(color(255,128,0));
      break;
    case 3: // mono gray fill
      scene.setStroke(true);
      scene.setStroke(color(0));
      scene.setFill(true);
      scene.setFill(color(192));
      break;
    case 5: // white, nostroke, different light
      noLights();
      ambientLight(96, 96, 96);
      pointLight(0,128,255, 0,0,0);
      PVector dir1 = new PVector(-1, +2, -3); dir1.normalize();
      directionalLight(100,100,100, dir1.x, dir1.y, dir1.z);
      PVector dir2 = new PVector(+1, +2, 2); dir2.normalize();
      directionalLight(100,100,100, dir2.x, dir2.y, dir2.z);
      scene.setStroke(false);
      scene.setFill(true);
      scene.setFill(color(255));
      break;
    default:
//      scene.enableStyle();
      break;
    }
    

    // render scene
    shape(scene);
    
    // screen-aligned 2D HUD
    PeasyCam.beginHUD(this.g, w, h);
    rectMode(CORNER);
    fill(0);
    rect(0, 0, 60, 23);
    fill(255,128,0);
    text("cam "+ID, 10, 15);
    PeasyCam.endHUD(this.g);
  }
  

  
  
  
  
  
  
  
  public static void main(String args[]) {
    PApplet.main(new String[] { MultiView.class.getName() });
  }
  
}

