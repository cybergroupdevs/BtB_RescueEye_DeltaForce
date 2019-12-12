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

def getshapes(path):
    print(path)
    detector = dlib.get_frontal_face_detector()
    predictor = dlib.shape_predictor(g.PREDICTOR_MODEL_PATH)
    img = dlib.load_rgb_image(path)
    dets = detector(img, 1)
    if len(dets) > 0:
        return predictor(img, dets[0])
    return None 

def probtoindex(input):
    print(input)
    if input == 'active' or input == 'activetest':
        return 0
    else:
        return 1
        
def train(model, folder):
    x_train = np.empty([0, 68 , 2], dtype = int)
    y_train = np.empty([0], dtype = int)
    prob = probtoindex(folder)
    path = glob.glob(os.path.join(g.IMAGE_PATH , folder,  "*.jpg"))
    i = 0
    for f in path:
        i = i + 1
        shapes = getshapes(f)
        if shapes != None:
            x = np.empty([68 , 2], dtype = int)
            for b in range(68):
                x[b][0] = shapes.part(b).x
                x[b][1] = shapes.part(b).y
            x_train = np.insert(x_train,len(x_train),x,axis=0)
            y_train = np.insert(y_train,len(y_train),prob,axis=0)
        if i > 2:
            break
    if len(x_train) > 0:
        model.fit(x_train, y_train, epochs=10)
    else:
        print('no data to train')
        
            

def savemodel(model):
    t = datetime.datetime.now().strftime("%Y%m%d-%H%M%S")
    tfpath = os.path.join(g.TENSOR_MODEL_PATH,  t + g.TENSOR_MODEL_SUFFIX)
    if not os.path.exists(tfpath):
        os.makedirs(tfpath)
    model.save(tfpath) 
    converter = tf.lite.TFLiteConverter.from_saved_model(tfpath)
    tflite_model = converter.convert()
    open( tfpath + '/' + g.TENSOR_LITE_MODEL_FILENAME, "wb").write(tflite_model)

def loadmodel(file):
    if file != None :
      m  = tf.keras.models.load_model(file)
      return m
    else:
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

def learn(video,mode):
    totalimages = 1#evi.ConvertVideoToFrame(video, mode, None)
    if(totalimages > 0):
        list_of_folders = glob.glob(os.path.join(g.TENSOR_MODEL_PATH,  '*' + g.TENSOR_MODEL_SUFFIX))
        latest_file = None
        if len(list_of_folders) > 0:
            latest_file = max(list_of_folders, key=os.path.getctime)

        model = loadmodel(latest_file)
        model.summary()
        train(model, mode)
        savemodel(model)

#learn('videos/WIN_20191211_16_03_04_Pro.mp4', 'active')