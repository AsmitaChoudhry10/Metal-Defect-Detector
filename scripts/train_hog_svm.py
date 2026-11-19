from pathlib import Path
import csv, json
import cv2
import numpy as np
from sklearn.pipeline import make_pipeline
from sklearn.preprocessing import StandardScaler
from sklearn.svm import SVC
from sklearn.metrics import accuracy_score, classification_report, confusion_matrix
import joblib

ROOT = Path(__file__).resolve().parents[1]
SPLITS = ROOT / 'data' / 'processed' / 'splits'
CLASSES = ['crazing','inclusion','patches','pitted_surface','rolled-in_scale','scratches']
HOG = cv2.HOGDescriptor((200,200),(40,40),(20,20),(20,20),9)

def load(name):
    X=[]; y=[]
    with (SPLITS / f'{name}.csv').open(encoding='utf-8', newline='') as f:
        for row in csv.DictReader(f):
            image=cv2.imread(str(ROOT / row['image']), cv2.IMREAD_GRAYSCALE)
            if image is None: raise RuntimeError(f"Cannot read {row['image']}")
            image=cv2.resize(image,(200,200),interpolation=cv2.INTER_AREA)
            X.append(HOG.compute(image).reshape(-1))
            y.append(row['filename_class'])
    return np.asarray(X,dtype=np.float32), np.asarray(y)

train_x,train_y=load('train')
val_x,val_y=load('validation')
test_x,test_y=load('test')
model=make_pipeline(StandardScaler(),SVC(kernel='linear',C=1.0,class_weight='balanced',probability=True,random_state=2026))
model.fit(train_x,train_y)
val_pred=model.predict(val_x)
test_pred=model.predict(test_x)
report=classification_report(test_y,test_pred,labels=CLASSES,output_dict=True,zero_division=0)
summary={'model':'HOG + standardized linear SVM','classes':CLASSES,'train_count':len(train_y),'validation_count':len(val_y),'test_count':len(test_y),'validation_accuracy':float(accuracy_score(val_y,val_pred)),'test_accuracy':float(accuracy_score(test_y,test_pred)),'test_macro_f1':float(report['macro avg']['f1-score']),'test_per_class':{c:{'precision':report[c]['precision'],'recall':report[c]['recall'],'f1':report[c]['f1-score'],'support':report[c]['support']} for c in CLASSES},'confusion_matrix':confusion_matrix(test_y,test_pred,labels=CLASSES).tolist(),'warning':'Closed-set defect classification only; dataset has no defect-free samples and does not validate plumbing fitting images.'}
(ROOT/'models').mkdir(exist_ok=True)
joblib.dump({'model':model,'classes':CLASSES,'feature':'OpenCV HOG 200x200'},ROOT/'models'/'neu_hog_svm.joblib')
(ROOT/'reports'/'baseline-results.json').write_text(json.dumps(summary,indent=2),encoding='utf-8')
print(json.dumps(summary,indent=2))
