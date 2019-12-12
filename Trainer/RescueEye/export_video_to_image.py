# It will export images from video, images will be generated only if face detected

# Library for Image Extract from Video
import cv2

# Library for Face Detection And Face Coordinate Extraction
import dlib

# Library for Os Level Operation
import os

# Library for Date Time Operation
import datetime

# Module for Getting Global Level Settings
import global_config as g

# Load Dlib Face Detection Module
detector = dlib.get_frontal_face_detector()

# Load Face Detection Modle
predictor = dlib.shape_predictor(g.PREDICTOR_MODEL_PATH)


# Convert Video To Frame
# videopath : video file path, 
# mode : action or inaction , 
# rotate : None Or cv2.ROTATE_180 or Angle Supported By CV2 Library
def ConvertVideoToFrame(videopath, mode, rotate):

    time = datetime.datetime.now().strftime(g.DATE_TIME_FORMAT)
    print(videopath)
    # Get Video From File
    cap = cv2.VideoCapture(videopath)
    i=0
    # Path to save frame
    path = g.IMAGE_PATH + '/' + mode
    if not os.path.exists(path):
        os.makedirs(path)
    # If Valid Video And Have Frames
    while(cap.isOpened()):
        # Get Frame from Video
        ret, frame = cap.read()
        print('creating frame')
        
        # Check If Frame Roatation Required
        if rotate != None:
            frame = cv2.rotate(frame, rotate)
        
        # Get Face
        faces = detector(frame, 1)
        # Check If Face Found
        if len(faces) > 0:
            print('facefound')
            
            # file name
            filename =  mode + '_' + time + '_' + str(i)+'.jpg'
            
            # save image to path
            cv2.imwrite(path + '/' + filename, frame)
            i+=1
            print(path + '/' + filename)
        else:
            print('no facefound')
    if i > 0:
        print('Total Image :' + i)
    cap.release()
    return i

# Example
#ConvertVideoToFrame('./videos/Skype_Video1.mp4', g.PROBABILITY_ACTIVE, None)
#ConvertVideoToFrame('./videos/Skype_Video1.mp4', g.PROBABILITY_ACTIVE, cv2.ROTATE_90_CLOCKWISE)
#ConvertVideoToFrame('./videos/Skype_Video2.mp4', g.PROBABILITY_INACTIVE, cv2.ROTATE_180)