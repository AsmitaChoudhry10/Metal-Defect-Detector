from pathlib import Path
import csv, json
import cv2, numpy as np
from sklearn.metrics import accuracy_score, classification_report
ROOT=Path(__file__).resolve().parents[1]
classes=['crazing','inclusion','patches','pitted_surface','rolled-in_scale','scratches']
hog=cv2.HOGDescriptor((200,200),(40,40),(20,20),(20,20),9)
def load(part):
 X=[]; y=[]
 with (ROOT/'data/processed/splits'/f'{part}.csv').open(encoding='utf-8') as f:
  for r in csv.DictReader(f):
   im=cv2.imread(str(ROOT/r['image']),0)
   X.append(hog.compute(cv2.resize(im,(200,200))).reshape(-1))
   y.append(classes.index(r['filename_class']))
 return np.asarray(X,dtype=np.float32),np.asarray(y,dtype=np.int32)
X,y=load('train'); V,vy=load('validation'); T,ty=load('test')
svm=cv2.ml.SVM_create(); svm.setType(cv2.ml.SVM_C_SVC); svm.setKernel(cv2.ml.SVM_LINEAR); svm.setC(1.0); svm.train(X,cv2.ml.ROW_SAMPLE,y)
_,vp=svm.predict(V); _,tp=svm.predict(T)
(ROOT/'android-app/app/src/main/assets').mkdir(parents=True,exist_ok=True)
svm.save(str(ROOT/'android-app/app/src/main/assets/neu_hog_svm.xml'))
pred=tp.reshape(-1).astype(int)
out={'model':'OpenCV HOG + linear C-SVC','class_id_order':classes,'train':len(y),'validation_accuracy':float(accuracy_score(vy,vp.reshape(-1).astype(int))),'test_accuracy':float(accuracy_score(ty,pred)),'test_report':classification_report(ty,pred,labels=list(range(6)),target_names=classes,output_dict=True,zero_division=0),'warning':'Closed-set only; no defect-free training examples. Not validated on photographed fitting.'}
(ROOT/'reports/opencv-mobile-model.json').write_text(json.dumps(out,indent=2),encoding='utf-8'); print(json.dumps({k:v for k,v in out.items() if k!='test_report'},indent=2))
