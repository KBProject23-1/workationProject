---
name: Feature request
about: Suggest an idea for this project
title: ''
labels: ''
assignees: ''

---

name: "✨ Feature"
description: "새로운 기능 추가"
title: "[FEAT] "
labels: ["feature"]
body:
  - type: textarea
    attributes:
      label: 📄 설명
      description: 새로운 기능에 대한 설명을 작성해 주세요.
      placeholder: 자세히 적을수록 좋습니다!
    validations:
      required: true

  - type: textarea
    attributes:
      label: ✅ 작업할 내용
      description: 할 일을 체크박스 형태로 작성해주세요.
      placeholder: 
        - [ ] 항목 1
        - [ ] 항목 2
    validations:
      required: true

  - type: textarea
    attributes:
      label: 🙋🏻 참고 자료
      description: 참고 자료가 있다면 작성해 주세요.
      placeholder: 관련 링크, API 명세서, 디자인 시안 등
    validations:
      required: false
