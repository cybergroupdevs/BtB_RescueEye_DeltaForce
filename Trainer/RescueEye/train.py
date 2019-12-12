from __future__ import absolute_import, division, print_function, unicode_literals
import sys
import os
import dlib
import glob
import numpy as np
import functools
import datetime
import tensorflow as tf
from tensorflow import keras
import global_config as g
import export_video_to_image as evi

# get face
def getface(path):
    # load dlib face detector
    detector = dlib.get_frontal_face_detector()

    # Get face predictor
    predictor = dlib.shape_predictor(g.PREDICTOR_MODEL_PATH)
    img = dlib.load_rgb_image(path)
    # Get faces
    dets = detector(img, 1)
    if len(dets) > 0:
        # return face
        return predictor(img, dets[0])
    return None 

# convert string to index for classification
def classifierindex(input):
    print(input)
    if input == 'active' or input == 'activetest':
        return 0
    else:
        return 1

# train model        
def train(model, mode):
    # define empty 3d model
    x_train = np.empty([0, 68 , 2], dtype = int)
    y_train = np.empty([0], dtype = int)

    prob = classifierindex(mode)
    path = glob.glob(os.path.join(g.IMAGE_PATH , mode,  "*.jpg"))
    i = 0
    for f in path:
        i = i + 1
        # get face
        face = getface(f)
        if face != None:
            x = np.empty([68 , 2], dtype = int)
            # get xy coordinates
            for b in range(68):
                x[b][0] = face.part(b).x
                x[b][1] = face.part(b).y
            x_train = np.insert(x_train,len(x_train),x,axis=0)
            y_train = np.insert(y_train,len(y_train),prob,axis=0)
        # for testing
        #if i > 2:
        #    break
    if len(x_train) > 0:
        # train a model
        model.fit(x_train, y_train, epochs=10)
    else:
        print('no data to train')
        
            
# Save Model
def savemodel(model):
    t = datetime.datetime.now().strftime("%Y%m%d-%H%M%S")
    tfpath = os.path.join(g.TENSOR_MODEL_PATH,  t + g.TENSOR_MODEL_SUFFIX)
    if not os.path.exists(tfpath):
        os.makedirs(tfpath)
    # save tensorflow model
    model.save(tfpath) 

    # convert tensorflow model to tensorflow lite
    converter = tf.lite.TFLiteConverter.from_saved_model(tfpath)
    tflite_model = converter.convert()
    
    # save tensorflow lite model
    open( tfpath + '/' + g.TENSOR_LITE_MODEL_FILENAME, "wb").write(tflite_model)

# load existing model or create new model
def loadmodel(folder):
    if folder != None :
        # Load Model
      m  = tf.keras.models.load_model(folder)
      return m
    else:
        # Define Model
        m = tf.keras.models.Sequential([
            tf.keras.layers.Flatten(input_shape=(68,2)),
            tf.keras.layers.Dense(10, activation='relu'),
            tf.keras.layers.Dropout(0.2),
            tf.keras.layers.Dense(10, activation='relu'),
            tf.keras.layers.Dropout(0.2),
            tf.keras.layers.Dense(10, activation='relu'),
            tf.keras.layers.Dropout(0.2),
            tf.keras.layers.Dense(2, activation='softmax')
        ])
        m.compile(optimizer='adam', loss='sparse_categorical_crossentropy', metrics=['accuracy'])
        return m

# Train Model From Uploaded Video
def learn(video,mode):
    # Convert Video To Image
    totalimages = evi.ConvertVideoToFrame(video, mode, None)
    if(totalimages > 0):

        list_of_folders = glob.glob(os.path.join(g.TENSOR_MODEL_PATH,  f'*{g.TENSOR_MODEL_SUFFIX}'))
        latest_folder = None
        if len(list_of_folders) > 0:
            # Get Latest Model
            latest_folder = max(list_of_folders, key=os.path.getctime)
        # Load Model
        model = loadmodel(latest_folder)
        model.summary()
        # Train
        train(model, mode)
        # Save Model
        savemodel(model)

# sample
# learn('samples/WIN_20191211_16_03_04_Pro.mp4', 'active')