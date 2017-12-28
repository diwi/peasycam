import java.util.Locale;
import peasycam3.PeasyCam;
import peasycam3.org.apache.commons.math.geometry.Rotation;
import peasycam3.org.apache.commons.math.geometry.Vector3D;



  //
  // Advanced QuadView setup
  //
  // Four independent Cameras are created, each owns a different canvas.
  //
  // Each camera/canvas comes with individual features
  // - default reset states
  // - ortho, projection
  // - shading, wire, lights
  //
  // When holding the CTRL-Key, the currently focused camera drives all other cameras
  // by applying its current state to them.
  //
  //
  // The Window can be dynamically resized.
  //
  
  
  
  
  int view_w = 1280;
  int view_h = 1024;

  final int NX = 2;
  final int NY = 2;
  PeasyCam[] cameras = new PeasyCam[NX * NY];
  
  public void settings() {
    size(view_w, view_h, P2D);
    smooth(0);
  }
  
  public void setup() {
    surface.setResizable(true);
    
    resizeScene();
  }
  

  public boolean resizeScene(){

    view_w = width;
    view_h = height;
    
    int border = 5;
    
    int dimx = (view_w / NX);
    int dimy = (view_h / NY);
    
    boolean rval = false;
    PGraphics canvas;

    for(int y = 0; y < NY; y++){
      for(int x = 0; x < NX; x++){
        int id = y * NX + x;
        
        int cw = dimx - border * 2;
        int ch = dimy - border * 2;
        int cx = x * dimx + border;
        int cy = y * dimy + border;
        
        boolean resize = false;
        if(cameras[id] == null){
          PeasyCam cam = new PeasyCam(this, 250);
          
          // user calls update manually during program flow
          cam.setAutoUpdate(false);
          
          if(id == 0) {
            // FRONT VIEW, XZ, ortho
            cam.setRotation(new Rotation(Vector3D.plusI, -PI/2), 0);
          }
          if(id == 1) {
            // SIDE VIEW, YZ, ortho
            cam.setRotation(new Rotation(), 0);
            cam.rotateZ( PI/2);
            cam.rotateX(-PI/2);
          }
          if(id == 2) {
            // Custom VIEW, perspective
            cam.setRotation(new Rotation(), 0);
            cam.rotateZ(+1*PI/10);
            cam.rotateX(-3*PI/8);
          }
          if(id == 3) {
            // TOP VIEW, XY, perspective
            cam.setRotation(new Rotation(), 0);
          }
          
          // save current state as reset state
          cam.pushResetState();
          
          cameras[id] = cam;
          
          resize = true;
        } else {
          canvas = cameras[id].getCanvas();
          if(canvas.width != cw || canvas.height != ch){
            canvas.dispose();
            resize = true;
          }
        }
        
        if(resize){
          canvas = createGraphics(cw, ch, P3D);
          canvas.smooth(8);
          cameras[id].setCanvas(canvas);
          cameras[id].setViewport(cx, cy, cw, ch);
        }
        
        rval |= resize;
      }
    }
    
    return rval;
  }
  
  
  public void draw(){
    resizeScene();
    
    // render scene
    for(int i = 0; i < cameras.length; i++){
      displayScene(cameras[i], i);
    }
   
    // display results
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
     
    // set modelview
    if(locked != null && locked != cam){
      PeasyCam.State state = locked.getState();
      cam.setState(state, 0);
    }
    
    // we manipulate the camera state by hand ...
    // so we manually update the camera to be sure we got the latest state correct
    cam.update();
    
    // set projection
    canvas.perspective(90 * DEG_TO_RAD, w/(float)h, 1, 5000);
    if(ID == 0 || ID == 1){
      float oscale = cam.getDistance() * 0.0035f;
      float ox = w/2 * oscale;
      float oy = h/2 * oscale;
      canvas.ortho(-ox, +ox, -oy, +oy, -10000, 10000);
      cam.setPanScale( 0.1 / cam.getDistance());
    }
    
 
    // scene
    canvas.background(48);
    canvas.rectMode(CENTER);
    
    
    // point-light: cr,cg,cb, tx,ty,tz, rz
    float[][] pointlights = {
         {255, 128,  64,  -100, -150, 40,  frameCount * 0.0025f},
         { 64, 128, 255,    20, +100, 80,  frameCount * 0.005f },
         {255, 255,  64,   100,    0, 20,  frameCount * 0.008f },
    };
    
    // show point-lights as spheres
    for(int i = 0; i < pointlights.length; i++){
      float[] pl = pointlights[i];
      canvas.pushMatrix();
      canvas.noStroke();
      canvas.rotateZ  (pl[6]);
      canvas.translate(pl[3], pl[4], pl[5]);
      canvas.fill     (pl[0], pl[1], pl[2]);
      canvas.sphere(5);
      canvas.popMatrix();
    }
    
    // lights
    canvas.ambientLight(64, 64, 64);
    
    for(int i = 0; i < pointlights.length; i++){
      float[] pl = pointlights[i];
      canvas.pushMatrix();
      canvas.rotateZ   (pl[6]);
      canvas.translate (pl[3], pl[4], pl[5]);
      canvas.pointLight(pl[0], pl[1], pl[2], 0, 0, 0);
      canvas.popMatrix();
    }

    int gray = 220;
    int wire_col = 0;
    if(ID == 1){
      canvas.noLights();
      canvas.lights();
    }

    // ground ... tiled, for gouraud shading
    canvas.strokeWeight(0.5f);
    canvas.fill(160);
    if(ID == 1){
      canvas.fill(gray);
      canvas.stroke(0);
    }
    int tile_size = 15;
    int tnx = 30;
    int tny = 30;
    canvas.pushMatrix();
    canvas.translate(-tnx * tile_size * 0.5f, -tny * tile_size * 0.5f, 0);
    for(int y = 0; y < tny; y++){
      for(int x = 0; x < tnx; x++){
        canvas.rect(x * tile_size, y * tile_size, tile_size, tile_size);
      }
    }
    canvas.popMatrix();

    // gizmo
    canvas.strokeWeight(2);
    canvas.stroke(255, 64,  0); canvas.line(0,0,0,100,0,0);
    canvas.stroke( 32,255, 32); canvas.line(0,0,0,0,100,0);
    canvas.stroke(  0, 64,255); canvas.line(0,0,0,0,0,100);
    
    canvas.strokeWeight(0.5f);
    
    // box1
    canvas.pushMatrix();
    canvas.translate(80,80,80);
    canvas.noStroke();
    canvas.fill(255,128,0);
    if(ID == 1){
      canvas.fill(gray);
      canvas.stroke(0);
    }
    if(ID == 3){
      canvas.noFill();
      canvas.stroke(wire_col);
    }
    canvas.box(50);
    canvas.popMatrix();
    
    // box2
    canvas.pushMatrix();
    canvas.translate(40,-60, 60);
    canvas.rotateZ(frameCount * 0.005f);
    canvas.noStroke();
    canvas.fill(0, 64, 160);
    if(ID == 1){
      canvas.fill(gray);
      canvas.stroke(0);
    }
    if(ID == 3){
      canvas.noFill();
      canvas.stroke(wire_col);
    }
    canvas.box(60, 30, 90);
    canvas.popMatrix();
    
    // sphere
    canvas.pushMatrix();
    canvas.translate(-40, -40, 40);
    canvas.scale(1,1,2);
    canvas.noStroke();
    canvas.fill(128,220,0);
    if(ID == 1){
      canvas.fill(gray);
      canvas.stroke(0);
    }
    if(ID == 3){
      canvas.noFill();
      canvas.stroke(wire_col);
    }
    canvas.sphere(40);
    canvas.popMatrix();
    
    // screen-aligned 2D HUD
    cam.beginHUD();
    {
      canvas.rectMode(CORNER);
      canvas.noStroke();
      canvas.fill(0, 32); 
      canvas.rect(   0,   0, 80, 40);
      
      float    distance = cam.getDistance();
      float[]  center   = cam.getCenter();
      Rotation rotation = cam.getRotation();
      
      String txt_ID       = String.format(Locale.ENGLISH, "cam %d", ID);
      String txt_dist     = String.format(Locale.ENGLISH, "distance %5.1f", distance);
      String txt_lookat   = String.format(Locale.ENGLISH, "center %4.1f, %4.1f, %4.1f", center[0], center[1], center[2]);
      String txt_rotation = PeasyCam.Utils.getRotationString(rotation);
      
      if(ID == 0) txt_ID  = String.format(Locale.ENGLISH, "cam %d (front, ortho, lights)", ID);
      if(ID == 1) txt_ID  = String.format(Locale.ENGLISH, "cam %d (left, ortho, wire, shaded)", ID);
      if(ID == 2) txt_ID  = String.format(Locale.ENGLISH, "cam %d (free, perspective, lights)", ID);
      if(ID == 3) txt_ID  = String.format(Locale.ENGLISH, "cam %d (top, perspective, wire, lights)", ID);
      
      int tx = 10;
      int ty = 5;
      canvas.fill(255);
      canvas.textSize(11);
      canvas.text(txt_ID      , tx, ty += 12);
      canvas.text(txt_dist    , tx, ty += 12);
      canvas.text(txt_lookat  , tx, ty += 12);
      canvas.text(txt_rotation, tx, ty += 12);
    }
    cam.endHUD();
    
    if(canvas != this.g) canvas.endDraw();
  }
  
  
  

  // CTRL/mousePressed logic to lock one camera
  PeasyCam locked = null;
  boolean CTRL_DOWN = false;
  boolean MOUSE_DOWN = false;

  public void keyPressed(){
    if(keyCode == CONTROL){
      CTRL_DOWN = true;
    }
    lockCam();
  }
  
  public void keyReleased(){
    if(keyCode == CONTROL){
      CTRL_DOWN = false;
      locked = null;
    }
  }
  
  public void mousePressed(){
    MOUSE_DOWN = true;
    lockCam();
  }
  
  public void mouseReleased(){
    MOUSE_DOWN = false;
    if(!CTRL_DOWN && !MOUSE_DOWN){
      locked = null;
    }
  }
  
  public void lockCam(){
    if(CTRL_DOWN && MOUSE_DOWN){
      for(int i = 0; i < cameras.length; i++){
        if(cameras[i].insideViewport(mouseX, mouseY)){
          locked = cameras[i];
          break;
        }
      }
    }
  }

