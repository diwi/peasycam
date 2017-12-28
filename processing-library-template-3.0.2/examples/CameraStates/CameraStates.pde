import java.util.Locale;
import java.util.Stack;
import peasycam3.PeasyCam;
import peasycam3.org.apache.commons.math.geometry.Rotation;
import peasycam3.org.apache.commons.math.geometry.Vector3D;

  
// 
// This examples shows the use of PeasyCam.State 
// 
// the internal peasycam state is defined by 
//   - distance (to center)
//   - center   (where the camera looks at)
//   - rotation (around the center)
//
// A state can be saved and applied at any time to any
// existing camera instance. (e.g. see the QuadView example)
// 
// Additionally, a State can be create manually and either be applied
// to an existing camera, or be used to create a new camera.
//
//
// Controls:
//
// [1, 9] + ALT_PRESSED ... save state
// [1, 9]               ... apply saved state
//
// a ... save state to the stack (push)
// x ... apply state from the stack (pull)
//
// y ... save state to internal copy
// x ... apply internal copy
//


PeasyCam cam;

public void settings() {
  size(1280, 720, P3D);
  smooth(8);
}

public void setup() {
  cam = new PeasyCam(this, 300);
  textFont(createFont("SourceCodePro-Regular", 12));
}


public void draw() {
  
  // projection
  perspective(60 * DEG_TO_RAD, width/(float)height, 1, 5000);
  
  // BG
  background(32);
  
  // gizmo
  strokeWeight(1);
  stroke(255, 32,  0); line(0,0,0,100,0,0);
  stroke( 32,255, 32); line(0,0,0,0,100,0);
  stroke(  0, 32,255); line(0,0,0,0,0,100);

  // objects
  strokeWeight(0.5f);
  stroke(0);
  
  pushMatrix();
  translate(50, 50, 0);
  fill(128,255,0);
  box(50, 50, 25);
  popMatrix();
  
  pushMatrix();
  translate(-50, -50, 0);
  fill(255,0,128);
  box(50, 50, 25);
  popMatrix();
  
  pushMatrix();
  translate(+50, -50, 0);
  fill(0,128,255);
  box(50, 50, 25);
  popMatrix();
  
  pushMatrix();
  translate(-50, +50, 0);
  fill(255,255,0);
  box(50, 50, 25);
  popMatrix();
  
  // HeadsUpDisplay
  displayHUD();
}



// display current state specs
void displayHUD(){
  cam.beginHUD();
 
  fill(0,128);
  strokeWeight(1);
  stroke(0);
  rect(10, 10, 250, 80, 5);
  
  PeasyCam.State state = cam.getState();
  double   distance = state.distance;
  Vector3D center   = state.center; 
  Rotation rotation = state.rotation;

  String txt_dist     = String.format(Locale.ENGLISH, "Distance:   %7.2f", distance);
  String txt_center   = String.format(Locale.ENGLISH, "Center:     %+7.2f, %+7.2f, %+7.2f", center.getX(), center.getY(), center.getZ());
  String txt_rotation = String.format(Locale.ENGLISH, "Rotation:   %+6.3f, %+6.3f, %+6.3f, %+6.3f", rotation.getQ0(), rotation.getQ1(), rotation.getQ2(), rotation.getQ3() );

  int tx = 25;
  int ty = 35;
  int tdy = 18;
  fill(255);
  text(txt_dist    , tx, ty); ty += tdy;
  text(txt_center  , tx, ty); ty += tdy;
  text(txt_rotation, tx, ty); ty += tdy;

  cam.endHUD();
}





// save 9 states with the number keys
PeasyCam.State state[] = new PeasyCam.State[9];

// save a stack of states
Stack<PeasyCam.State> stack = new Stack<PeasyCam.State>();


public void keyPressed(KeyEvent ev){

  // set/get states via the array
  if(key >= '1' && key <= '9'){
    int id = key - '1'; // [0, 9]
    if(ev.isAltDown()){
      state[id] = cam.getState();
    } else {
      cam.setState(state[id], 1000);
    }
  }

  // set/get internal copy state
  if(key == 'y') cam.pushState();
  if(key == 'x') cam.popState();
  
  // set/get states via the stack
  // could be used to build a more complex camera path
  if(key == 'a'                    ) stack.push(cam.getState());
  if(key == 's' && !stack.isEmpty()) cam.setState(stack.pop(), 1000);
}



