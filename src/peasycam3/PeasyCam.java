/**
 * 
 * The PeasyCam3 Processing library, which provides an easy-peasy
 * camera for 3D sketching.
 *
 *   Copyright 2008 Jonathan Feinberg
 *   Copyright 2017 Thomas Diewald
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * 
 */


package peasycam3;

import java.util.Locale;

import peasycam3.org.apache.commons.math.geometry.CardanEulerSingularityException;
import peasycam3.org.apache.commons.math.geometry.Rotation;
import peasycam3.org.apache.commons.math.geometry.RotationOrder;
import peasycam3.org.apache.commons.math.geometry.Vector3D;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.event.KeyEvent;
import processing.event.MouseEvent;
import processing.opengl.PGraphicsOpenGL;

/**
 * 
 * @author Jonathan Feinberg
 * @author Thomas Diewald (v3.0.0, 2017)
 * 
 */
public class PeasyCam {
  
  public static final String VERSION = "301";
  
  static final Vector3D LOOK = Vector3D.plusK; // [0, 0, 1]
  static final Vector3D UP   = Vector3D.plusJ; // [0, 1, 0]
  
  // rotation constraint flags
  static final int YAW   = 0x01;
  static final int PITCH = 0x02;
  static final int ROLL  = 0x04;
  static final int ALL   = YAW | PITCH | ROLL;
  
  int SHIFT_CONSTRAINT = 0; // applied when pressing the shift key
  int FIXED_CONSTRAINT = 0; // applied, when set by user and SHIFT_CONSTRAINT is 0
  int DRAG_CONSTRAINT  = 0; // depending on SHIFT_CONSTRAINT and FIXED_CONSTRAINT, default is ALL
  
  
  // render target
  PGraphics canvas;
  
  // custom user data
  public Object user;
  
  // viewport for the mouse-pointer [x,y,w,h]
  float[] viewport = new float[4];

  
  // camera state
  State state;
  State state_reset;
  State state_pushed;
  
  
  // scale/sensitivity/speed for mouse control
  double scale_rotation  = 0.001;
  double scale_pan       = 0.0005;
  double scale_zoom      = 0.001;
  double scale_zoomwheel = 20.0;
  
  // zoom limit
  double distance_min_limit = 0.001;
  double distance_min       = distance_min_limit;
  double distance_max       = Double.MAX_VALUE;
  
 
  // registered draw
  boolean auto_update = true;
  
  // other stuff
  long default_interpolation_time = 300;

  // damped state changes
  public DampedAction dampedZoom = new DampedAction() { void action(double value) { zoom   (value * getZoomMult()    ); } };
  public DampedAction dampedPanX = new DampedAction() { void action(double value) { panX   (value * getPanMult()     ); } };
  public DampedAction dampedPanY = new DampedAction() { void action(double value) { panY   (value * getPanMult()     ); } };
  public DampedAction dampedRotX = new DampedAction() { void action(double value) { rotateX(value * getRotationMult()); } };
  public DampedAction dampedRotY = new DampedAction() { void action(double value) { rotateY(value * getRotationMult()); } };
  public DampedAction dampedRotZ = new DampedAction() { void action(double value) { rotateZ(value * getRotationMult()); } };
  

  // interpolated state changes
  public Interpolation<Rotation> timedRot  = new Interpolation<Rotation>() { void action(double t) { setRotation(valA, valB, t); } };
  public Interpolation<Vector3D> timedPan  = new Interpolation<Vector3D>() { void action(double t) { setCenter  (valA, valB, t); } };
  public Interpolation<Double>   timedzoom = new Interpolation<Double>  () { void action(double t) { setDistance(valA, valB, t); } };
  
  // mouse actions
  public MouseAction m_left   = new MouseAction() { public void apply() { mouseDragRotate();    } };
  public MouseAction m_center = new MouseAction() { public void apply() { mouseDragPan();       } };
  public MouseAction m_right  = new MouseAction() { public void apply() { mouseDragZoom();      } };
  public MouseAction m_wheel  = new MouseAction() { public void apply() { mouseWheelZoom();     } };

  

  /** PApplet */
  public PeasyCam(PApplet papplet) {
    this(papplet.g);
  }
  /** PGraphics */
  public PeasyCam(PGraphics canvas) {
    this(canvas, 500);
  }
  
  
  /** PApplet, distance */
  public PeasyCam(PApplet papplet, double distance) {
    this(papplet.g, distance);
  }
  /** PGraphics, distance */
  public PeasyCam(PGraphics canvas, double distance) {
    this(canvas, distance, 0, 0, 0);
  }

  
  /** PApplet, distance, center */
  public PeasyCam(PApplet papplet, double distance, double cx, double cy, double cz) {
    this(papplet.g, distance, cx, cy, cz);
  }
  /** PGraphics, distance, center */
  public PeasyCam(PGraphics canvas, double distance, double cx, double cy, double cz) {
    this(canvas, new State(distance, new Vector3D(cx, cy, cz), new Rotation()));
  }
  
  
  /** PApplet, distance, center, angles-XYZ */
  public PeasyCam(PApplet papplet, double distance, double cx, double cy, double cz, double rx, double ry, double rz) {
    this(papplet.g, distance, cx, cy, cz, rx, ry, rz);
  }
  /** PGraphics, distance, center, angles-XYZ*/
  public PeasyCam(PGraphics canvas, double distance, double cx, double cy, double cz, double rx, double ry, double rz) {
    this(canvas, new State(distance, new Vector3D(cx, cy, cz), Utils.newRotationFromAnglesXYZ(rx, ry, rz)));
  }
  
  
  /** PApplet, State */
  public PeasyCam(PApplet papplet, State state) {
    this(papplet.g, state);
  }
  /** PGraphics, State */
  public PeasyCam(PGraphics canvas, State state) {
    this.canvas = canvas;
    
    this.state        = new State(state);
    this.state_reset  = new State(state);
    this.state_pushed = new State(state);
    
    setViewport(0, 0, canvas.width, canvas.height);
    
    // registered callbacks
    canvas.parent.registerMethod("draw"      , this);
    canvas.parent.registerMethod("dispose"   , this);
    canvas.parent.registerMethod("mouseEvent", this);
    canvas.parent.registerMethod("keyEvent"  , this);
  }
  
  
  
  // mouse and keyevetn fields
  boolean DRAG_ACTIVE = false;
  double  mx = 0, pmx = 0, dmx = 0;
  double  my = 0, pmy = 0, dmy = 0;
  double  mwheel = 0;
  
  // PApplet registered mouseEvent-callback
  public void mouseEvent(final MouseEvent me) {
    
    final int count   = me.getCount();
    final int action  = me.getAction();
    final int button  = me.getButton();
    
    final boolean LMB = (button == PConstants.LEFT  );
    final boolean MMB = (button == PConstants.CENTER);
    final boolean RMB = (button == PConstants.RIGHT );
      
    // previous mouse location
    pmx = mx;
    pmy = my;
    
    // current mouse location
    mx = me.getX();
    my = me.getY();
    
    // mouse distance (inverted)
    dmx = -(mx - pmx);
    dmy = -(my - pmy);
    
    // mouse wheel
    mwheel = count;

    switch (action) {
    
    case MouseEvent.PRESS:
      if(insideViewport(mx, my)){
        DRAG_ACTIVE = true;
        SHIFT_CONSTRAINT = 0;
        dmx = dmy = 0;
        pmx = mx;
        pmy = my;
      }
      break;
    
    case MouseEvent.WHEEL:
      if(insideViewport(mx, my)){
        if(m_wheel != null) m_wheel.apply();
      }
      break;
      
    case MouseEvent.CLICK:
      if(insideViewport(mx, my) && count == 2){
        reset();
      }
      break;
        
    case MouseEvent.DRAG:
      
      if(!DRAG_ACTIVE) break;
      
      if (me.isShiftDown() && SHIFT_CONSTRAINT == 0 && Math.abs(dmx - dmy) > 1) {
        SHIFT_CONSTRAINT = Math.abs(dmx) > Math.abs(dmy) ? YAW : PITCH;
      }
      
      // define constraint by increasing priority
      DRAG_CONSTRAINT = ALL;
      if(FIXED_CONSTRAINT > 0) DRAG_CONSTRAINT = FIXED_CONSTRAINT;
      if(SHIFT_CONSTRAINT > 0) DRAG_CONSTRAINT = SHIFT_CONSTRAINT;
      

      if ((MMB || (LMB && me.isMetaDown()))) {
        if(m_center != null) m_center.apply();
      } else if (LMB) {
        if(m_left != null) m_left.apply();
      } else if (RMB) {
        if(m_right != null) m_right.apply();
      }
      break;
      
    case MouseEvent.RELEASE:
      DRAG_ACTIVE = false;
      SHIFT_CONSTRAINT = 0;
      pmx = mx;
      pmy = my;
      break;
    }
  }
    
  // PApplet registered keyEvent-callback
  public void keyEvent(final KeyEvent ke) {
    int action = ke.getAction();
    int keycode = ke.getKeyCode();
    boolean shift_key = (keycode == PConstants.SHIFT);
  
    switch (action) {
    case KeyEvent.PRESS:
      break;
    case KeyEvent.RELEASE:
      if(shift_key) SHIFT_CONSTRAINT = 0;
      break;
    }
    
  }
  
  // PApplet registered draw-callback
  public void draw() {
    if(auto_update){
      update();
    }
  }
  
  // PApplet registered dispose-callback
  public void dispose(){
    release();
  }

  // main camera update method
  public void update(){
    
    boolean b_update = false;
    
    // update damped actions (direct manips)
    b_update |= dampedRotX.update();
    b_update |= dampedRotY.update();
    b_update |= dampedRotZ.update();
    b_update |= dampedZoom.update();
    b_update |= dampedPanX.update();
    b_update |= dampedPanY.update();
    
    // interpolated actions have lower priority then damped actions
    if(b_update){
      timedRot .stop();
      timedPan .stop();
      timedzoom.stop();
    } else {
      timedRot .update();
      timedPan .update();
      timedzoom.update();
    }
    
    apply();
  }
  
  
  public void release(){
    // no resources to release 
  }
  
  
  public boolean getAutoUpdate(){
    return auto_update;
  }
  
  public void setAutoUpdate(boolean status){
    auto_update = status;
  }
  
  
  
  
  // eye/center/up buffer
  float[] camEYE = new float[3];
  float[] camLAT = new float[3];
  float[] camRUP = new float[3];
  
  public void apply() {
    apply(canvas);
  }

  public void apply(PGraphics canvas) {
    if (canvas.isGL() && canvas.is3D()) {
      
      camEYE = getPosition(camEYE);   
      camLAT = getCenter  (camLAT);
      camRUP = getUpVector(camRUP);
      
      canvas.camera(camEYE[0], camEYE[1], camEYE[2],
                    camLAT[0], camLAT[1], camLAT[2],
                    camRUP[0], camRUP[1], camRUP[2]);
    }
  }
  

  
  


  





  
  
  


  
  
  
  
  

  



  
  
  
  //
  // mouse state changes
  //
  void mouseWheelZoom() {
    dampedZoom.addForce(mwheel * scale_zoomwheel);
  }
  
  void mouseDragZoom() {
    dampedZoom.addForce(-dmy);
  }
  
  void mouseDragPan() {
    dampedPanX.addForce((DRAG_CONSTRAINT & YAW  ) > 0 ? dmx : 0);
    dampedPanY.addForce((DRAG_CONSTRAINT & PITCH) > 0 ? dmy : 0);
  }
  
  void mouseDragRotate() {
    // mouse [-1, +1]
    double mxNdc = Math.min(Math.max((mx - viewport[0]) / viewport[2], 0), 1) * 2 - 1;
    double myNdc = Math.min(Math.max((my - viewport[1]) / viewport[3], 0), 1) * 2 - 1;

    if ((DRAG_CONSTRAINT & YAW) > 0) {
      dampedRotY.addForce(+dmx * (1.0 - myNdc * myNdc));
    }
    if ((DRAG_CONSTRAINT & PITCH) > 0) {
      dampedRotX.addForce(-dmy * (1.0 - mxNdc * mxNdc));
    }
    if ((DRAG_CONSTRAINT & ROLL) > 0) {
      dampedRotZ.addForce(-dmx * myNdc);
      dampedRotZ.addForce(+dmy * mxNdc);
    }
  }
  
  
  
  
  
  //
  // damped multipliers
  //
  double getZoomMult(){
    return state.distance * scale_zoom;
  }
  
  double getPanMult(){
    return state.distance * scale_pan;
  }
  
  double getRotationMult(){
    return Math.pow(Utils.log10(1 + state.distance), 0.5) * scale_rotation;
  }
  
  
  
  
 

  //
  // damped state changes
  //
  public void zoom(double dz){
    double distance_tmp = state.distance + dz;
    
    if(distance_tmp < distance_min) {
      distance_tmp = distance_min;
      dampedZoom.stop();
    }
    if(distance_tmp > distance_max) {
      distance_tmp = distance_max;
      dampedZoom.stop();
    }
    
    state.distance = distance_tmp;
  }
  
  public void panX(double dx) {
    if(dx != 0) state.center = state.center.add(state.rotation.applyTo(new Vector3D(dx, 0, 0)));
  }
  
  public void panY(double dy) {
    if(dy != 0) state.center = state.center.add(state.rotation.applyTo(new Vector3D(0, dy, 0)));
  }
  
  public void pan(double dx, double dy) {
    panX(dx);
    panY(dx);
  }

  public void rotateX(double rx) {
    rotate(Vector3D.plusI, rx);
  }

  public void rotateY(double ry) {
    rotate(Vector3D.plusJ, ry);
  }

  public void rotateZ(double rz) {
    rotate(Vector3D.plusK, rz);
  }
  
  public void rotate(Vector3D axis, double angle) {
    if(angle != 0) state.rotation = state.rotation.applyTo(new Rotation(axis, angle));
  }
  
  



  
  
  //
  // interpolated state changes
  //
  /** Interpolated Distance transition */
  public void setDistance(double valA, double valB, double t) {
    state.distance = Utils.mix(valA, valB, Utils.smootherstep(t));
  }
  /** Interpolated Center transition */
  public void setCenter(Vector3D valA, Vector3D valB, double t) {
    state.center = Utils.mix(valA, valB, Utils.smootherstep(t));
  }
  /** Interpolated Rotation transition */
  public void setRotation(Rotation valA, Rotation valB, double t) {
    state.rotation = Utils.slerp(valA, valB, t);
  }
  
  
  
  

  //
  // CAMERA DISTANCE
  //
  public void setDistanceMin(double distance_min) {
    this.distance_min = Math.max(distance_min, distance_min_limit);
    zoom(0); // update, to ensure new minimum
  }

  public void setDistanceMax(double distance_max) {
    this.distance_max = distance_max;
    zoom(0); // update, to ensure new maximum
  }
  
  public void setDistance(double distance) {
    setDistance(distance, default_interpolation_time);
  }

  public void setDistance(double distance, long duration) {
    timedzoom.start(state.distance, distance, duration, dampedZoom);
  }

  public float getDistance() {
    return (float) state.distance;
  }

  
  //
  // CAMERA CENTER / LOOK AT
  //
  public void setCenter(double x, double y, double z) {
    setCenter(x, y, z, default_interpolation_time);
  }

  public void setCenter(double x, double y, double z, long duration) {
    setCenter(new Vector3D(x, y, z), duration);
  }
  
  public void setCenter(Vector3D center, long duration) {
    timedPan.start(state.center, center, duration, dampedPanX, dampedPanY);
  }
  
  public float[] getCenter() {
    return getCenter(null);
  }
  
  public float[] getCenter(float[] dst) {
    if(dst == null || dst.length != 3){
      dst = new float[3];
    }
    dst[0] = (float) state.center.getX();
    dst[1] = (float) state.center.getY();
    dst[2] = (float) state.center.getZ();
    return dst;
  }
  
  
  
  
  //
  // CAMERA ROTATION
  //
  public void setRotation(Rotation rotation) {
    setRotation(rotation, default_interpolation_time);
  }
  
  public void setRotation(Rotation rotation, long duration) {
    timedRot.start(state.rotation, rotation, duration, dampedRotX, dampedRotY, dampedRotZ);
  }
  
  public Rotation getRotation() {
    return state.rotation;
  }
  


  //
  // CAMERA POSITION/EYE
  //
  public float[] getPosition() {
    return getPosition(null);
  }
  public float[] getPosition(float[] dst) {
    if(dst == null || dst.length != 3){
      dst = new float[3];
    }
    Vector3D pos = state.rotation.applyTo(LOOK).scalarMultiply(state.distance).add(state.center);
    dst[0] = (float) pos.getX();
    dst[1] = (float) pos.getY();
    dst[2] = (float) pos.getZ();
    return dst;
  }

  
  //
  // CAMERA UP
  //
  public float[] getUpVector(){
    return getUpVector(null);
  }
  
  public float[] getUpVector(float[] dst) {
    if(dst == null || dst.length != 3){
      dst = new float[3];
    }
    Vector3D up = state.rotation.applyTo(UP);
    dst[0] = (float) up.getX();
    dst[1] = (float) up.getY();
    dst[2] = (float) up.getZ();
    return dst;
  }
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  //
  // STATE (rotation, center, distance)
  //
  public PeasyCam.State getState() {
    return new State(state);
  }
  
  public void setState(PeasyCam.State other) {
    setState(other, default_interpolation_time);
  }

  public void setState(PeasyCam.State other, long duration) {
    if(other != null){
      setDistance(other.distance, duration);
      setCenter  (other.center  , duration);
      setRotation(other.rotation, duration);
    }
  }



  public PeasyCam.State pushState(){
    return (state_pushed = getState());
  }
  public void popState(){
    popState(default_interpolation_time);
  }
  public void popState(long duration){
    setState(state_pushed, duration);
  }
  
  
  public PeasyCam.State pushResetState(){
    return (state_reset = getState());
  }
  public void reset(){
    reset(default_interpolation_time);
  }
  public void reset(long duration){
    setState(state_reset, duration);
  }
  

  
  

  
  
  
  
  public PGraphics getCanvas(){
    return canvas;
  }
  
  public void setCanvas(PGraphics canvas){
    this.canvas = canvas;
  }
  
  public float[] getViewport(){
    return viewport;
  }
  
  public void setViewport(float x, float y, float w, float h){
    viewport[0] = x; viewport[1] = y;
    viewport[2] = w; viewport[3] = h;
  }
  
  public void setViewport(float[] xywh){
    viewport[0] = xywh[0]; viewport[1] = xywh[1];
    viewport[2] = xywh[2]; viewport[3] = xywh[3];
  }
  
  public boolean insideViewport(double x, double y){
    float x0 = viewport[0], x1 = x0 + viewport[2];
    float y0 = viewport[1], y1 = y0 + viewport[3];
    return (x > x0) && (x < x1) && (y > y0) && (y < y1);
  }
  
  
  
  
  
  
  public void setRotationScale(double scale_rotation){
    this.scale_rotation = scale_rotation;
  }
  
  public void setPanScale(double scale_pan){
    this.scale_pan = scale_pan;
  }
  
  public void setZoomScale(double scale_zoom){
    this.scale_zoom = scale_zoom;
  }
  
  public float getRotationScale(){
    return (float) this.scale_rotation;
  }
  
  public float getPanScale(){
    return (float) this.scale_pan;
  }
  
  public float getZoomScale(){
    return (float) this.scale_zoom;
  }
  
  public float getWheelScale() {
    return (float) this.scale_zoomwheel;
  }

  public void setWheelScale(double wheelScale) {
    this.scale_zoomwheel = wheelScale;
  }
  
  public void setDamping(double damping){
    dampedZoom.damping = damping;
    dampedPanX.damping = damping;
    dampedPanY.damping = damping;
    dampedRotX.damping = damping;
    dampedRotY.damping = damping;
    dampedRotZ.damping = damping;
  }

  public void setDefaultInterpolationTime(long duration){
    this.default_interpolation_time = duration;
  }
  
  public long getDefaultInterpolationTime(){
    return this.default_interpolation_time;
  }

  public void setRotationConstraint(boolean yaw, boolean pitch, boolean roll) {
    FIXED_CONSTRAINT  = 0;
    FIXED_CONSTRAINT |= yaw   ? YAW   : 0;
    FIXED_CONSTRAINT |= pitch ? PITCH : 0;
    FIXED_CONSTRAINT |= roll  ? ROLL  : 0;
  }



  
 
  boolean pushedLights = false;

  /**
   * 
   * begin screen-aligned 2D-drawing.
   * 
   * <pre>
   * beginHUD()
   *   disabled depth test
   *   disabled lights
   *   ortho
   *   ... your code is executed here ...
   * endHUD()
   * </pre>
   * 
   */
  public void beginHUD() {
    canvas.hint(PConstants.DISABLE_DEPTH_TEST);
    canvas.pushMatrix();
    canvas.resetMatrix();
    // 3D is always GL (in processing 3), so this check is probably redundant.
    if (canvas.isGL() && canvas.is3D()) {
      PGraphicsOpenGL pgl = (PGraphicsOpenGL) canvas;
      pushedLights = pgl.lights;
      pgl.lights = false;
      pgl.pushProjection();
      canvas.ortho(0, canvas.width, -canvas.height, 0, -Float.MAX_VALUE, +Float.MAX_VALUE);
    }
  }

  /**
   * 
   * end screen-aligned 2D-drawing.
   * 
   */
  public void endHUD() {
    if (canvas.isGL() && canvas.is3D()) {
      PGraphicsOpenGL pgl = (PGraphicsOpenGL) canvas;
      pgl.popProjection();
      pgl.lights = pushedLights;
    }
    canvas.popMatrix();
    canvas.hint(PConstants.ENABLE_DEPTH_TEST);
  }
  
  
  
  
  
  
  
  
  
  

  
  static public interface MouseAction {
    void apply();
  }
  
  
 
  static public abstract class Interpolation<T> {
    
    T valA, valB;
    double timer, duration;
    boolean active;
  
    void start(T valA, T valB, long duration, DampedAction ... actions) {
      for(DampedAction da : actions){
        da.stop();
      }
      this.valA = valA;
      this.valB = valB;
      this.duration = duration;
      this.timer = System.currentTimeMillis();
      this.active = duration > 0;
      if(!active){
        action(1);
      }
    }
    
    void update() {
      if(active){
        double t = (System.currentTimeMillis() - timer) / duration;
        if (t > 0.995) {
          action(1);
          stop();
        } else {
          action(t);
        }
      }
    }
    
    void stop() {
      active = false;
    }
    
    abstract void action(double t);
  }

  
  
  
  static public abstract class DampedAction {
    
    double value = 0;
    public double damping = 0.85;

    void addForce(double force) {
      value += force;
    }

    boolean update() {
      boolean active = (value*value) > 0.000001;
      if (active){
        action(value);
        value *= damping;
      } else {
        stop();
      }
      return active;
    }

    void stop() {
      value = 0;
    }

    abstract void action(double value);
  }


  

  
  
  
  /**
   * deep copy of camera state
   */
  static public class State{
    
    public double distance;
    public  Vector3D center;
    public Rotation rotation;
    
    public State(double distance, Vector3D center, Rotation rotation){
      this.distance = distance;
      this.center   = center.scalarMultiply(1);
      this.rotation = new Rotation(rotation.getQ0(), rotation.getQ1(), rotation.getQ2(), rotation.getQ3(), false);
    }
    
    public State(State other){
      this(other.distance, other.center, other.rotation);
    }
    
    public String toString(){
      // center
      double cx = center.getX();
      double cy = center.getY();
      double cz = center.getZ();
      // rotation
      double q0 = rotation.getQ0(); 
      double q1 = rotation.getQ1();
      double q2 = rotation.getQ2();
      double q3 = rotation.getQ3();
      
      String s0 = String.format(Locale.ENGLISH, "PeasyCam State");
      String s1 = String.format(Locale.ENGLISH, "  > distance: %8.3f", distance);
      String s2 = String.format(Locale.ENGLISH, "  > center  : %8.3f, %8.3f, %8.3f", cx, cy, cz);
      String s3 = String.format(Locale.ENGLISH, "  > rotation: %8.3f, %8.3f, %8.3f, %8.3f",q0, q1, q2, q3);
      
      return s0 + "\n" + s1 + "\n" + s2 + "\n" + s3;
    }
    
  }
  
  
  

  
  
  

  
  
  static public class Utils {
    
    static final public double LOG2N = Math.log(2.0);
    
    static public double log2(double v){
      return Math.log(v) / LOG2N;
    }
    
    static public double log10(double v){
      return Math.log10(v);
    }
    
    static public double smootherstep(double x) {
      return x * x * x * (x * (x * 6 - 15) + 10);
    }
    
    static public double smoothstep(double x) {
      return x * x * (3 - 2 * x);
    }
    
    
    static public double mix(double a, double b, double t) {
      return a * (1 - t) + b * t ;
    }

    static public Vector3D mix(Vector3D a, Vector3D b, double t) {
      return new Vector3D( mix(a.getX(), b.getX(), t), 
                           mix(a.getY(), b.getY(), t),
                           mix(a.getZ(), b.getZ(), t));
    }
    
   
    // Thanks to Michael Kaufmann <mail@michael-kaufmann.ch> for improvements to this function.
    static public Rotation slerp(Rotation a, Rotation b, double t) {
      double a0 = a.getQ0(), a1 = a.getQ1(), a2 = a.getQ2(), a3 = a.getQ3();
      double b0 = b.getQ0(), b1 = b.getQ1(), b2 = b.getQ2(), b3 = b.getQ3();

      double cosTheta = a0 * b0 + a1 * b1 + a2 * b2 + a3 * b3;
      if (cosTheta < 0) {
        b0 = -b0;
        b1 = -b1;
        b2 = -b2;
        b3 = -b3;
        cosTheta = -cosTheta;
      }

      double theta = Math.acos(cosTheta);
      double sinTheta = Math.sqrt(1.0 - cosTheta * cosTheta);

      double w1, w2;
      if (sinTheta > 0.001) {
        w1 = Math.sin((1.0 - t) * theta) / sinTheta;
        w2 = Math.sin(t * theta) / sinTheta;
      } else {
        w1 = 1.0 - t;
        w2 = t;
      }
      return new Rotation(w1 * a0 + w2 * b0, w1 * a1 + w2 * b1, w1 * a2 + w2 * b2, w1* a3 + w2 * b3, true);
    }
    
    
    
    
    static public Rotation newRotationFromAnglesXYZ(double[] rXYZ){
      return newRotationFromAnglesXYZ(rXYZ[0], rXYZ[1], rXYZ[2]);
    }
    
    static public Rotation newRotationFromAnglesXYZ(double rx, double ry, double rz){
      double ax = -0.5 * rx;
      double ay = -0.5 * ry;
      double az = -0.5 * rz;
      
      Rotation rX = new Rotation(Math.cos(ax), Math.sin(ax), 0, 0, false);
      Rotation rY = new Rotation(Math.cos(ay), 0, Math.sin(ay), 0, false);
      Rotation rZ = new Rotation(Math.cos(az), 0, 0, Math.sin(az), false);
      
      return rX.applyTo(rY.applyTo(rZ));
    }
    
    
    static public double[] getAnglesXYZ(Rotation rotation) {
      try {
        return rotation.getAngles(RotationOrder.XYZ);
      } catch (final CardanEulerSingularityException e) {
      }
      try {
        return rotation.getAngles(RotationOrder.YXZ);
      } catch (final CardanEulerSingularityException e) {}
      try {
        return rotation.getAngles(RotationOrder.ZXY);
      } catch (final CardanEulerSingularityException e) {
      }
      return new double[] { 0, 0, 0 };
    }
    
     
    static public String getRotationString(Rotation rotation){
      return String.format(Locale.ENGLISH, "Rotation: %+6.3f, %+6.3f, %+6.3f, %+6.3f" , 
          rotation.getQ0(), 
          rotation.getQ1(), 
          rotation.getQ2(), 
          rotation.getQ3());
    }
    
    
    
  }
  
  

  
  
  
  
  

}
