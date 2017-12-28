import peasycam3.PeasyCam;

//
// Simple QuadView setup
//
// 4 independent Cameras are created, each owns a new canvas.
// 


final int NX = 2;
final int NY = 2;
PeasyCam[] cameras = new PeasyCam[NX * NY];

public void settings() {
  size(1280, 1024, P2D); // 2D
  smooth(8);
}

public void setup() {
  int border = 5;
  int dimx = (width / NX);
  int dimy = (height / NY);

  for(int y = 0; y < NY; y++){
    for(int x = 0; x < NX; x++){
      int id = y * NX + x;
      int cw = dimx - border * 2;
      int ch = dimy - border * 2;
      int cx = x * dimx + border;
      int cy = y * dimy + border;
      
      PGraphics canvas = createGraphics(cw, ch, P3D); // 3D

      cameras[id] = new PeasyCam(canvas, 400);
      cameras[id].setViewport(cx, cy, cw, ch);
    }
  }
}


public void draw(){
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

  // begin draw
  if(canvas != this.g) canvas.beginDraw();
   
  // scene
  canvas.background(32);
  canvas.rectMode(CENTER);
  
  canvas.lights();
  
  // ground ... tiled, for gouraud shading
  canvas.strokeWeight(0.5f);
  canvas.fill(220);
  canvas.rect(0, 0, 400, 400);

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
  if(ID == 3){
    canvas.stroke(0);
    canvas.noFill();
  }
  canvas.box(50);
  canvas.popMatrix();
  
  // box2
  canvas.pushMatrix();
  canvas.translate(40,-60, 60);
  canvas.rotateZ(frameCount * 0.005f);
  canvas.noStroke();
  canvas.fill(0, 64, 160);
  if(ID == 3){
    canvas.stroke(0);
    canvas.noFill();
  }
  canvas.box(60, 30, 90);
  canvas.popMatrix();
  
  // sphere
  canvas.pushMatrix();
  canvas.translate(-40, -40, 40);
  canvas.scale(1,1,2);
  canvas.noStroke();
  canvas.fill(128,220,0);
  if(ID == 3){
    canvas.stroke(0);
    canvas.noFill();
  }
  canvas.sphere(40);
  canvas.popMatrix();
  
  // screen-aligned 2D HUD
  cam.beginHUD();
  canvas.fill(255);
  canvas.text("cam "+ID, 10, 15);
  cam.endHUD();
  
  // end draw
  if(canvas != this.g) canvas.endDraw();
}

